package com.gestionreportes.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardMetricasDTO {
    private long totalReportes;
    private long urgenciaAlta;
    private long finalizados;
}
