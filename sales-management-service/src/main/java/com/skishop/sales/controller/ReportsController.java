package com.skishop.sales.controller;

import com.skishop.sales.dto.response.ProductSalesReportResponse;
import com.skishop.sales.dto.response.ReturnReportResponse;
import com.skishop.sales.dto.response.SalesReportResponse;
import com.skishop.sales.dto.response.ShippingReportResponse;
import com.skishop.sales.service.ReportsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * レポート API コントローラー
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reports API", description = "レポート・分析API")
public class ReportsController {

    private final ReportsService reportsService;

    /**
     * 売上レポート取得
     */
    @GetMapping("/sales")
    @Operation(summary = "売上レポート取得", description = "指定期間の売上レポートを取得します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "売上レポートの取得成功"),
        @ApiResponse(responseCode = "400", description = "パラメータが不正です"),
        @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SALES_MANAGER') or hasRole('ANALYST')")
    public ResponseEntity<SalesReportResponse> getSalesReport(
            @Parameter(description = "開始日") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "終了日") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = "グループ化方法 (DAILY, WEEKLY, MONTHLY)") @RequestParam(defaultValue = "DAILY") String groupBy) {
        
        log.info("Getting sales report from {} to {}, groupBy: {}", fromDate, toDate, groupBy);
        SalesReportResponse response = reportsService.getSalesReport(groupBy, fromDate.toString(), toDate.toString());
        return ResponseEntity.ok(response);
    }

    /**
     * 商品別売上レポート取得
     */
    @GetMapping("/products")
    @Operation(summary = "商品別売上レポート取得", description = "指定期間の商品別売上レポートを取得します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "商品別売上レポートの取得成功"),
        @ApiResponse(responseCode = "400", description = "パラメータが不正です"),
        @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SALES_MANAGER') or hasRole('PRODUCT_MANAGER')")
    public ResponseEntity<ProductSalesReportResponse> getProductSalesReport(
            @Parameter(description = "開始日") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "終了日") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = "取得件数制限") @RequestParam(defaultValue = "50") int limit) {
        
        log.info("Getting product sales report from {} to {}, limit: {}", fromDate, toDate, limit);
        ProductSalesReportResponse response = reportsService.getProductSalesReport("DAILY", fromDate.toString(), toDate.toString(), null, null);
        return ResponseEntity.ok(response);
    }

    /**
     * 売上レポートエクスポート
     */
    @GetMapping("/export/sales")
    @Operation(summary = "売上レポートエクスポート", description = "指定期間の売上レポートをファイル形式でエクスポートします")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "ファイルエクスポート成功"),
        @ApiResponse(responseCode = "400", description = "パラメータが不正です"),
        @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('SALES_MANAGER')")
    public ResponseEntity<byte[]> exportSalesReport(
            @Parameter(description = "開始日") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "終了日") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = "エクスポート形式 (CSV, EXCEL, PDF)") @RequestParam(defaultValue = "CSV") String format) {
        
        log.info("Exporting sales report from {} to {}, format: {}", fromDate, toDate, format);
        byte[] fileData = reportsService.exportSalesReport(fromDate, toDate, format);
        
        HttpHeaders headers = new HttpHeaders();
        String filename = String.format("sales_report_%s_%s.%s", fromDate, toDate, format.toLowerCase());
        headers.setContentDispositionFormData("attachment", filename);
        
        MediaType mediaType = switch (format.toUpperCase()) {
            case "CSV" -> MediaType.TEXT_PLAIN;
            case "EXCEL" -> MediaType.APPLICATION_OCTET_STREAM;
            case "PDF" -> MediaType.APPLICATION_PDF;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
        
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(mediaType)
                .body(fileData);
    }

    /**
     * 配送実績レポート取得
     */
    @GetMapping("/shipping")
    @Operation(summary = "配送実績レポート取得", description = "指定期間の配送実績レポートを取得します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "配送実績レポートの取得成功"),
        @ApiResponse(responseCode = "400", description = "パラメータが不正です"),
        @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('LOGISTICS_MANAGER')")
    public ResponseEntity<ShippingReportResponse> getShippingReport(
            @Parameter(description = "開始日") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "終了日") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = "運送会社フィルター") @RequestParam(required = false) String carrier) {
        
        log.info("Getting shipping report from {} to {}, carrier: {}", fromDate, toDate, carrier);
        ShippingReportResponse response = reportsService.getShippingReport(fromDate.toString(), toDate.toString(), carrier);
        return ResponseEntity.ok(response);
    }

    /**
     * 返品分析レポート取得
     */
    @GetMapping("/returns")
    @Operation(summary = "返品分析レポート取得", description = "指定期間の返品分析レポートを取得します")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "返品分析レポートの取得成功"),
        @ApiResponse(responseCode = "400", description = "パラメータが不正です"),
        @ApiResponse(responseCode = "403", description = "アクセス権限がありません")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER_SERVICE') or hasRole('QUALITY_MANAGER')")
    public ResponseEntity<ReturnReportResponse> getReturnReport(
            @Parameter(description = "開始日") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "終了日") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = "返品理由フィルター") @RequestParam(required = false) String reason) {
        
        log.info("Getting return report from {} to {}, reason: {}", fromDate, toDate, reason);
        ReturnReportResponse response = reportsService.getReturnReport(fromDate.toString(), toDate.toString(), reason);
        return ResponseEntity.ok(response);
    }
}
