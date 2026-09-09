package com.token.erp.controller;

import com.token.erp.common.PageResult;
import com.token.erp.common.Result;
import com.token.erp.entity.Location;
import com.token.erp.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/base/location")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('base:location')")
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/list")
    public Result<PageResult<Location>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String keyword) {
        return Result.success(PageResult.of(locationService.page(page, pageSize, warehouseId, keyword)));
    }

    @GetMapping("/byWarehouse")
    public Result<List<Location>> byWarehouse(@RequestParam Long warehouseId) {
        return Result.success(locationService.listByWarehouse(warehouseId));
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('base:location:create')")
    public Result<Location> create(@RequestBody Location location) {
        return Result.success(locationService.create(location));
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('base:location:update')")
    public Result<Location> update(@RequestBody Location location) {
        return Result.success(locationService.update(location));
    }
}
