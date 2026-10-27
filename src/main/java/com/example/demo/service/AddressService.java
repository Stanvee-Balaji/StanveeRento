package com.example.demo.service;

import com.example.demo.dto.AddressDto;
import com.example.demo.entity.AddressEntity;
import com.example.demo.repository.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Saved delivery addresses for Stanvee users. Owner is ALWAYS the username from the JWT.
 * Every lookup is by (id + username) so a user can never touch someone else's address.
 */
@Service
public class AddressService {

    private static final int MAX_ADDRESSES = 10;

    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    // ───────── READ ─────────

    @Transactional(readOnly = true)
    public AddressDto.AddressListResponse list(String username) {
        return buildList(username);
    }

    @Transactional(readOnly = true)
    public AddressDto.AddressResponse get(String username, UUID addressId) {
        return toResponse(findOwned(username, addressId));
    }

    /** Used by the "prepare order / confirm order" flow to validate and load the chosen address. */
    @Transactional(readOnly = true)
    public AddressEntity requireOwned(String username, UUID addressId) {
        return findOwned(username, addressId);
    }

    // ───────── ADD ─────────

    @Transactional
    public AddressDto.AddressResponse add(String username, AddressDto.AddressRequest req) {
        long count = addressRepository.countByUsername(username);
        if (count >= MAX_ADDRESSES) {
            throw new SuperAdminService.BadRequestException(
                    "Address book is full (max " + MAX_ADDRESSES + " addresses)");
        }

        // first address is always default
        boolean makeDefault = count == 0 || Boolean.TRUE.equals(req.getIsDefault());
        if (makeDefault) {
            addressRepository.clearDefault(username);
        }

        AddressEntity a = new AddressEntity();
        a.setUsername(username);
        apply(a, req);
        a.setDefaultAddress(makeDefault);
        return toResponse(addressRepository.save(a));
    }

    // ───────── UPDATE ─────────

    @Transactional
    public AddressDto.AddressResponse update(String username, UUID addressId, AddressDto.AddressRequest req) {
        AddressEntity a = findOwned(username, addressId);
        apply(a, req);

        if (Boolean.TRUE.equals(req.getIsDefault()) && !a.isDefaultAddress()) {
            addressRepository.clearDefault(username);
            a = findOwned(username, addressId); // reload (clearAutomatically detached it)
            apply(a, req);
            a.setDefaultAddress(true);
        }
        // isDefault=false on the current default is ignored: there must always be one default.
        return toResponse(addressRepository.save(a));
    }

    @Transactional
    public AddressDto.AddressListResponse setDefault(String username, UUID addressId) {
        AddressEntity a = findOwned(username, addressId);
        if (!a.isDefaultAddress()) {
            addressRepository.clearDefault(username);
            a = findOwned(username, addressId);
            a.setDefaultAddress(true);
            addressRepository.save(a);
        }
        return buildList(username);
    }

    // ───────── REMOVE ─────────

    @Transactional
    public AddressDto.AddressListResponse remove(String username, UUID addressId) {
        AddressEntity a = findOwned(username, addressId);
        boolean wasDefault = a.isDefaultAddress();
        addressRepository.delete(a);
        addressRepository.flush();

        if (wasDefault) {
            addressRepository.findFirstByUsernameOrderByCreatedAtDesc(username).ifPresent(next -> {
                next.setDefaultAddress(true);
                addressRepository.save(next);
            });
        }
        return buildList(username);
    }

    // ───────── INTERNAL ─────────

    private AddressEntity findOwned(String username, UUID addressId) {
        return addressRepository.findByIdAndUsername(addressId, username)
                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException(
                        "Address not found: " + addressId));
    }

    private void apply(AddressEntity a, AddressDto.AddressRequest r) {
        a.setLabel(r.getLabel().trim().toUpperCase());
        a.setFullName(r.getFullName().trim());
        a.setPhone(r.getPhone().trim());
        a.setAddressLine1(r.getAddressLine1().trim());
        a.setAddressLine2(blankToNull(r.getAddressLine2()));
        a.setLandmark(blankToNull(r.getLandmark()));
        a.setCity(r.getCity().trim());
        a.setState(r.getState().trim());
        a.setPincode(r.getPincode().trim());
        a.setCountry(r.getCountry() == null || r.getCountry().isBlank() ? "India" : r.getCountry().trim());
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private AddressDto.AddressListResponse buildList(String username) {
        List<AddressDto.AddressResponse> items = addressRepository
                .findByUsernameOrderByDefaultAddressDescCreatedAtDesc(username).stream()
                .map(this::toResponse)
                .toList();

        AddressDto.AddressListResponse r = new AddressDto.AddressListResponse();
        r.setUsername(username);
        r.setAddresses(items);
        r.setAddressCount(items.size());
        return r;
    }

    private AddressDto.AddressResponse toResponse(AddressEntity a) {
        AddressDto.AddressResponse r = new AddressDto.AddressResponse();
        r.setAddressId(a.getId());
        r.setLabel(a.getLabel());
        r.setFullName(a.getFullName());
        r.setPhone(a.getPhone());
        r.setAddressLine1(a.getAddressLine1());
        r.setAddressLine2(a.getAddressLine2());
        r.setLandmark(a.getLandmark());
        r.setCity(a.getCity());
        r.setState(a.getState());
        r.setPincode(a.getPincode());
        r.setCountry(a.getCountry());
        r.setIsDefault(a.isDefaultAddress());
        r.setCreatedAt(a.getCreatedAt());
        r.setUpdatedAt(a.getUpdatedAt());
        return r;
    }
}
