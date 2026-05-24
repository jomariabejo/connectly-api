package com.jomariabejo.connectly_api.supplier_api.controller;

import com.jomariabejo.connectly_api.supplier_api.dto.*;
import com.jomariabejo.connectly_api.supplier_api.service.GRNService;
import com.jomariabejo.connectly_api.service.AuthenticationService;
import com.jomariabejo.connectly_api.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goods-receipt-notes")
@RequiredArgsConstructor
@Slf4j
public class GRNController {

    private final GRNService grnService;
    private final AuthenticationService authenticationService;

    @PostMapping
    public ResponseEntity<GoodsReceiptNoteDto> createGRN(@RequestBody CreateGRNRequest request) {
        User user = authenticationService.getAuthenticatedUser();
        GoodsReceiptNoteDto grn = grnService.createGRN(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(grn);
    }

    @PostMapping("/{grnId}/items")
    public ResponseEntity<GRNItemDto> addReceivedItem(
            @PathVariable Long grnId,
            @RequestBody CreateGRNItemRequest request) {
        User user = authenticationService.getAuthenticatedUser();
        GRNItemDto item = grnService.addReceivedItem(grnId, request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @PutMapping("/{grnId}/inspect")
    public ResponseEntity<GoodsReceiptNoteDto> inspectGRN(@PathVariable Long grnId) {
        User user = authenticationService.getAuthenticatedUser();
        GoodsReceiptNoteDto grn = grnService.inspectGRN(grnId, user.getId());
        return ResponseEntity.ok(grn);
    }

    @GetMapping("/{grnId}")
    public ResponseEntity<GoodsReceiptNoteDto> getGRN(@PathVariable Long grnId) {
        GoodsReceiptNoteDto grn = grnService.getGRN(grnId);
        return ResponseEntity.ok(grn);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<GoodsReceiptNoteDto>> getGRNsByStatus(@PathVariable String status) {
        List<GoodsReceiptNoteDto> grns = grnService.getGRNsByStatus(status);
        return ResponseEntity.ok(grns);
    }
}
