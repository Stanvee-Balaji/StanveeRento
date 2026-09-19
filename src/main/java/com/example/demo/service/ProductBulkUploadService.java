package com.example.demo.service;

import com.example.demo.dto.ProductDto;
import com.example.demo.entity.CategoryEntity;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.InventoryRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

@Service
public class ProductBulkUploadService {

    private static final String[] HEADERS = {
        "Category Name", "Product Name", "Description", "Colour", "Occasion",
        "Per Day Price", "Weekend Price", "Security Deposit", "Offer Price",
        "Is Visible (TRUE/FALSE)", "Image URL 1", "Image URL 2", "Image URL 3",
        "Size", "SKU", "Condition", "Inventory Status"
    };

    private static final Set<String> CONDITIONS = Set.of("NEW", "GOOD", "FAIR", "POOR");
    private static final Set<String> STATUSES = Set.of("AVAILABLE", "RENTED", "CLEANING", "DAMAGED");

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;

    public ProductBulkUploadService(ProductService productService,
                                     CategoryRepository categoryRepository,
                                     InventoryRepository inventoryRepository) {
        this.productService = productService;
        this.categoryRepository = categoryRepository;
        this.inventoryRepository = inventoryRepository;
    }

    // ═══════════════════════════════════════════
    //  TEMPLATE
    // ═══════════════════════════════════════════

    public byte[] generateTemplate() {
        try (Workbook wb = WorkbookFactory.create(true)) {
            Sheet sheet = wb.createSheet("Products");
            Row header = sheet.createRow(0);
            CellStyle boldStyle = wb.createCellStyle();
            Font bold = wb.createFont();
            bold.setBold(true);
            boldStyle.setFont(bold);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(HEADERS[i]);
                c.setCellStyle(boldStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }
            // sample row
            Row sample = sheet.createRow(1);
            String[] example = {"Sarees", "Red Banarasi Silk Saree", "Elegant silk saree", "Red", "Wedding",
                "999", "1299", "3000", "799", "TRUE",
                "https://cdn.example.com/img1.jpg", "", "",
                "M", "SAR-RED-001", "NEW", "AVAILABLE"};
            for (int i = 0; i < example.length; i++) sample.createCell(i).setCellValue(example[i]);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new SuperAdminService.BadRequestException("Failed to generate template: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════
    //  PREVIEW (validate only, no DB writes)
    // ═══════════════════════════════════════════

    public ProductDto.BulkUploadPreviewResponse preview(MultipartFile file) {
        List<ParsedRow> rows = parseRows(file);
        List<ProductGroup> groups = groupRows(rows);

        List<ProductDto.BulkProductPreview> previews = new ArrayList<>();
        Set<String> skusInFile = new HashSet<>();

        for (ProductGroup g : groups) {
            ProductDto.BulkProductPreview p = new ProductDto.BulkProductPreview();
            p.setCategoryName(g.categoryName);
            p.setName(g.name);
            p.setPerDayPrice(g.perDayPrice);
            p.setInventoryCount(g.inventoryItems.size());
            p.setSkus(g.inventoryItems.stream().map(ProductDto.InventoryItem::getSku).toList());
            p.setSourceRowNumbers(g.sourceRows);

            List<ProductDto.BulkRowError> errors = validateGroup(g, skusInFile, false);
            p.setErrors(errors);
            p.setValid(errors.isEmpty());
            previews.add(p);
        }

        ProductDto.BulkUploadPreviewResponse resp = new ProductDto.BulkUploadPreviewResponse();
        resp.setTotalRowsRead(rows.size());
        resp.setTotalProductsParsed(groups.size());
        resp.setValidProductCount((int) previews.stream().filter(ProductDto.BulkProductPreview::isValid).count());
        resp.setInvalidProductCount(previews.size() - resp.getValidProductCount());
        resp.setProducts(previews);
        return resp;
    }

    // ═══════════════════════════════════════════
    //  IMPORT (actually creates products)
    // ═══════════════════════════════════════════

    public ProductDto.BulkImportResponse importFile(MultipartFile file, UUID actorId, String actorName,
                                                      String actorRole, String ip) {
        List<ParsedRow> rows = parseRows(file);
        List<ProductGroup> groups = groupRows(rows);
        Set<String> skusInFile = new HashSet<>();

        List<ProductDto.BulkImportRowOutcome> outcomes = new ArrayList<>();
        int created = 0, failed = 0;

        for (ProductGroup g : groups) {
            ProductDto.BulkImportRowOutcome outcome = new ProductDto.BulkImportRowOutcome();
            outcome.setCategoryName(g.categoryName);
            outcome.setName(g.name);

            List<ProductDto.BulkRowError> errors = validateGroup(g, skusInFile, true);
            if (!errors.isEmpty()) {
                outcome.setErrors(errors);
                outcome.setCreated(false);
                failed++;
                outcomes.add(outcome);
                continue;
            }

            try {
                CategoryEntity category = categoryRepository.findByNameIgnoreCaseAndDeletedAtIsNull(g.categoryName)
                        .orElseThrow(() -> new SuperAdminService.BadRequestException("Category not found"));

                ProductDto.CreateProductRequest req = new ProductDto.CreateProductRequest();
                req.setCategoryId(category.getId());
                req.setName(g.name);
                req.setDescription(g.description);
                req.setColour(g.colour);
                req.setOccasion(g.occasion);
                req.setPerDayPrice(g.perDayPrice);
                req.setWeekendPrice(g.weekendPrice);
                req.setSecurityDeposit(g.securityDeposit);
                req.setOfferPrice(g.offerPrice);
                req.setVisible(g.isVisible);
                req.setImages(g.images);
                req.setInventory(g.inventoryItems);

                var created_ = productService.create(req, actorId, actorName, actorRole, ip);
                outcome.setCreated(true);
                outcome.setProductId(created_.getId());
                created++;
            } catch (Exception e) {
                outcome.setCreated(false);
                outcome.getErrors().add(new ProductDto.BulkRowError(
                        g.sourceRows.get(0), "product", e.getMessage()));
                failed++;
            }
            outcomes.add(outcome);
        }

        ProductDto.BulkImportResponse resp = new ProductDto.BulkImportResponse();
        resp.setTotalProductsParsed(groups.size());
        resp.setCreatedCount(created);
        resp.setFailedCount(failed);
        resp.setResults(outcomes);
        return resp;
    }

    // ═══════════════════════════════════════════
    //  VALIDATION
    // ═══════════════════════════════════════════

    private List<ProductDto.BulkRowError> validateGroup(ProductGroup g, Set<String> skusInFile, boolean checkDb) {
        List<ProductDto.BulkRowError> errors = new ArrayList<>();
        int rowRef = g.sourceRows.get(0);

        if (isBlank(g.categoryName)) {
            errors.add(new ProductDto.BulkRowError(rowRef, "Category Name", "Required"));
        } else if (categoryRepository.findByNameIgnoreCaseAndDeletedAtIsNull(g.categoryName).isEmpty()) {
            errors.add(new ProductDto.BulkRowError(rowRef, "Category Name", "Category not found or inactive: " + g.categoryName));
        }
        if (isBlank(g.name)) errors.add(new ProductDto.BulkRowError(rowRef, "Product Name", "Required"));
        if (g.perDayPrice == null || g.perDayPrice.compareTo(BigDecimal.ZERO) < 0) {
            errors.add(new ProductDto.BulkRowError(rowRef, "Per Day Price", "Required and must be >= 0"));
        }
        if (g.inventoryItems.isEmpty()) {
            errors.add(new ProductDto.BulkRowError(rowRef, "Size/SKU", "At least one inventory row required"));
        }

        for (int i = 0; i < g.inventoryItems.size(); i++) {
            ProductDto.InventoryItem inv = g.inventoryItems.get(i);
            int r = g.sourceRows.get(i);
            if (isBlank(inv.getSize())) errors.add(new ProductDto.BulkRowError(r, "Size", "Required"));
            if (isBlank(inv.getSku())) {
                errors.add(new ProductDto.BulkRowError(r, "SKU", "Required"));
            } else {
                String sku = inv.getSku().trim().toUpperCase();
                if (!skusInFile.add(sku)) {
                    errors.add(new ProductDto.BulkRowError(r, "SKU", "Duplicate SKU within file: " + sku));
                } else if (checkDb && inventoryRepository.findBySkuIgnoreCase(sku).isPresent()) {
                    errors.add(new ProductDto.BulkRowError(r, "SKU", "SKU already exists in system: " + sku));
                }
            }
            if (inv.getCondition() != null && !CONDITIONS.contains(inv.getCondition().toUpperCase())) {
                errors.add(new ProductDto.BulkRowError(r, "Condition", "Invalid. Must be one of " + CONDITIONS));
            }
            if (inv.getStatus() != null && !STATUSES.contains(inv.getStatus().toUpperCase())) {
                errors.add(new ProductDto.BulkRowError(r, "Inventory Status", "Invalid. Must be one of " + STATUSES));
            }
        }
        return errors;
    }

    // ═══════════════════════════════════════════
    //  PARSING
    // ═══════════════════════════════════════════

    private List<ParsedRow> parseRows(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new SuperAdminService.BadRequestException("Excel file is required");
        }
        List<ParsedRow> rows = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) { // row 0 = header
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row, fmt)) continue;

                ParsedRow pr = new ParsedRow();
                pr.rowNumber = r + 1; // 1-based, human readable (header = row 1)
                pr.categoryName = str(row, 0, fmt);
                pr.name = str(row, 1, fmt);
                pr.description = str(row, 2, fmt);
                pr.colour = str(row, 3, fmt);
                pr.occasion = str(row, 4, fmt);
                pr.perDayPrice = num(row, 5, fmt);
                pr.weekendPrice = num(row, 6, fmt);
                pr.securityDeposit = num(row, 7, fmt);
                pr.offerPrice = num(row, 8, fmt);
                pr.isVisible = boolVal(row, 9, fmt);
                pr.image1 = str(row, 10, fmt);
                pr.image2 = str(row, 11, fmt);
                pr.image3 = str(row, 12, fmt);
                pr.size = str(row, 13, fmt);
                pr.sku = str(row, 14, fmt);
                pr.condition = str(row, 15, fmt);
                pr.status = str(row, 16, fmt);
                rows.add(pr);
            }
        } catch (IOException e) {
            throw new SuperAdminService.BadRequestException("Could not read Excel file: " + e.getMessage());
        }
        if (rows.isEmpty()) {
            throw new SuperAdminService.BadRequestException("No data rows found in file");
        }
        return rows;
    }

    /** Merge consecutive/any rows sharing Category+Name into a single product with multiple inventory items. */
    private List<ProductGroup> groupRows(List<ParsedRow> rows) {
        LinkedHashMap<String, ProductGroup> map = new LinkedHashMap<>();
        for (ParsedRow r : rows) {
            String key = (nullSafe(r.categoryName) + "||" + nullSafe(r.name)).toLowerCase();
            ProductGroup g = map.computeIfAbsent(key, k -> {
                ProductGroup ng = new ProductGroup();
                ng.categoryName = r.categoryName;
                ng.name = r.name;
                ng.description = r.description;
                ng.colour = r.colour;
                ng.occasion = r.occasion;
                ng.perDayPrice = r.perDayPrice;
                ng.weekendPrice = r.weekendPrice;
                ng.securityDeposit = r.securityDeposit;
                ng.offerPrice = r.offerPrice;
                ng.isVisible = r.isVisible == null || r.isVisible;
                ng.images = new ArrayList<>();
                for (String url : List.of(nullSafe(r.image1), nullSafe(r.image2), nullSafe(r.image3))) {
                    if (!url.isBlank()) {
                        ProductDto.ImageItem img = new ProductDto.ImageItem();
                        img.setImageUrl(url.trim());
                        ng.images.add(img);
                    }
                }
                return ng;
            });

            if (!isBlank(r.size) || !isBlank(r.sku)) {
                ProductDto.InventoryItem inv = new ProductDto.InventoryItem();
                inv.setSize(r.size);
                inv.setSku(r.sku);
                inv.setCondition(r.condition);
                inv.setStatus(r.status);
                inv.setActive(true);
                g.inventoryItems.add(inv);
            }
            g.sourceRows.add(r.rowNumber);
        }
        return new ArrayList<>(map.values());
    }

    // ---- small helpers ----

    private static boolean isRowEmpty(Row row, DataFormatter fmt) {
        for (int c = 0; c < HEADERS.length; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && !fmt.formatCellValue(cell).isBlank()) return false;
        }
        return true;
    }

    private static String str(Row row, int idx, DataFormatter fmt) {
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        String v = fmt.formatCellValue(cell).trim();
        return v.isEmpty() ? null : v;
    }

    private static BigDecimal num(Row row, int idx, DataFormatter fmt) {
        String v = str(row, idx, fmt);
        if (v == null) return null;
        try { return new BigDecimal(v.replace(",", "").trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private static Boolean boolVal(Row row, int idx, DataFormatter fmt) {
        String v = str(row, idx, fmt);
        if (v == null) return null;
        return v.trim().equalsIgnoreCase("TRUE");
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String nullSafe(String s) { return s == null ? "" : s.trim(); }

    private static class ParsedRow {
        int rowNumber;
        String categoryName, name, description, colour, occasion;
        BigDecimal perDayPrice, weekendPrice, securityDeposit, offerPrice;
        Boolean isVisible;
        String image1, image2, image3;
        String size, sku, condition, status;
    }

    private static class ProductGroup {
        String categoryName, name, description, colour, occasion;
        BigDecimal perDayPrice, weekendPrice, securityDeposit, offerPrice;
        boolean isVisible = true;
        List<ProductDto.ImageItem> images = new ArrayList<>();
        List<ProductDto.InventoryItem> inventoryItems = new ArrayList<>();
        List<Integer> sourceRows = new ArrayList<>();
    }
}