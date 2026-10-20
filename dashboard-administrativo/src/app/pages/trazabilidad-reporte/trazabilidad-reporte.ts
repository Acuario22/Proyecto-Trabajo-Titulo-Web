import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Urgencia, Sentimiento, URGENCIAS, SENTIMIENTOS } from '../panel-administrativo/panel-administrativo';

export interface PasoLineaTiempo {
  numero: number;
  titulo: string;
  fecha?: string;
  estado: 'completado' | 'en-proceso' | 'pendiente';
  descripcion: string;
}

export interface DetalleReporte {
  codigo: string;
  titulo: string;
  descripcion: string;
  fechaIngreso: string;
  urgencia: Urgencia;
  sentimiento: Sentimiento;
  estadoActual: string;
  detalleEstadoActual: string;
}

@Component({
  selector: 'app-trazabilidad-reporte',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './trazabilidad-reporte.html',
  styleUrl: './trazabilidad-reporte.css'
})
export class TrazabilidadReporteComponent implements OnInit {
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  readonly urgencias = URGENCIAS;
  readonly sentimientos = SENTIMIENTOS;

  reporte: DetalleReporte = {
    codigo: 'RPT-2024-0001',
    titulo: 'Reporte de Pavimentación',
    descripcion: 'Gran bache en la intersección de Av. Libertad con Calle 5, está causando daños a los vehículos y representa un peligro para los ciclistas que transitan diariamente.',
    fechaIngreso: '03/11/2024',
    urgencia: 'Alta',
    sentimiento: 'Enojado',
    estadoActual: 'En Proceso',
    detalleEstadoActual: 'Cuadrilla asignada. Reparación programada para el 10/11.'
  };

  pasos: PasoLineaTiempo[] = [
    {
      numero: 1,
      titulo: 'Iniciado',
      fecha: '03/11/2024',
      estado: 'completado',
      descripcion: 'Reporte recibido y registrado en el sistema.'
    },
    {
      numero: 2,
      titulo: 'En Proceso',
      fecha: '05/11/2024',
      estado: 'en-proceso',
      descripcion: 'Cuadrilla asignada. Reparación programada para el 10/11.'
    },
    {
      numero: 3,
      titulo: 'Finalizado',
      estado: 'pendiente',
      descripcion: 'Pendiente de ejecución.'
    }
  ];

  ngOnInit(): void {
    const codigoParam = this.route.snapshot.queryParamMap.get('codigo') || this.route.snapshot.paramMap.get('codigo');
    if (codigoParam) {
      this.reporte.codigo = codigoParam;
    }
  }

  volverAlPanel(): void {
    this.router.navigate(['/panel-administrativo']);
  }
}
