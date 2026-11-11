package com.olp.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

public class ReorderStructureRequest {

    @NotEmpty(message = "Ordered IDs list cannot be empty")
    private List<Integer> orderedIds = new ArrayList<>();

    public ReorderStructureRequest() {}

    public ReorderStructureRequest(List<Integer> orderedIds) {
        this.orderedIds = orderedIds != null ? orderedIds : new ArrayList<>();
    }

    public List<Integer> getOrderedIds() {
        return orderedIds;
    }

    public void setOrderedIds(List<Integer> orderedIds) {
        this.orderedIds = orderedIds != null ? orderedIds : new ArrayList<>();
    }
}

