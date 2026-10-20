import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Reporte {
  id: number;
  codigo: string;
  categoria: string;
  texto: string;
  direccion: string;
  fechaIngreso: string;
  urgencia: string;
  sentimiento: string;
  estado: string;
  nivelRiesgo: string;
  huboAjuste: boolean;
}

export interface CategoriaConteo {
  nombre: string;
  conteo: number;
}

export interface DashboardMetricas {
  totalReportes: number;
  urgenciaAlta: number;
  finalizados: number;
}

export interface CrearOrdenTrabajoRequest {
  cuadrilla: string;
  responsable: string;
  cantidadPersonal: number;
  vehiculo?: string;
  fechaVisita: string; // formato "yyyy-MM-dd"
  horaVisita: string; // formato "HH:mm"
  duracionEstimada: string;
  materiales?: string;
  instrucciones: string;
}

export interface OrdenTrabajoResponse {
  idOrden: number;
  idReporte: number;
  codigoReporte: string;
  cuadrilla: string;
  responsable: string;
  cantidadPersonal: number;
  vehiculo?: string;
  fechaVisita: string;
  horaVisita: string;
  duracionEstimada: string;
  materiales?: string;
  instrucciones: string;
  estadoReporte: string;
}

@Injectable({
  providedIn: 'root'
})
export class ReporteService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/reportes';

  /**
   * Obtiene la lista de categorías con su conteo respectivo desde el backend.
   * Endpoint: GET /api/reportes/categorias
   */
  obtenerCategorias(): Observable<CategoriaConteo[]> {
    return this.http.get<CategoriaConteo[]>(`${this.apiUrl}/categorias`);
  }

  /**
   * Obtiene todos los reportes de la base de datos sin filtros.
   * Endpoint: GET /api/reportes/todos
   */
  obtenerTodos(): Observable<Reporte[]> {
    return this.http.get<Reporte[]>(`${this.apiUrl}/todos`);
  }

  /**
   * Filtra reportes por categoría específica.
   * Endpoint: GET /api/reportes?categoria={categoria}
   */
  obtenerPorCategoria(categoria: string): Observable<Reporte[]> {
    const params = new HttpParams().set('categoria', categoria);
    return this.http.get<Reporte[]>(this.apiUrl, { params });
  }

  /**
   * Obtiene reportes según la categoría seleccionada. Si es 'Todos' o vacía,
   * llama al endpoint de todos los reportes; de lo contrario, filtra por categoría.
   */
  obtenerReportes(categoria?: string): Observable<Reporte[]> {
    if (categoria && categoria !== 'Todos') {
      return this.obtenerPorCategoria(categoria);
    }
    return this.obtenerTodos();
  }

  /**
   * Obtiene las métricas globales para las tarjetas de KPIs del dashboard.
   * Endpoint: GET /api/reportes/metricas
   */
  obtenerMetricas(): Observable<DashboardMetricas> {
    return this.http.get<DashboardMetricas>(`${this.apiUrl}/metricas`);
  }

  /**
   * Obtiene un reporte unitario por su ID numérico (PK).
   * Endpoint: GET /api/reportes/{id}
   */
  obtenerReportePorId(id: number): Observable<Reporte> {
    return this.http.get<Reporte>(`${this.apiUrl}/${id}`);
  }

  /**
   * Crea o actualiza una orden de trabajo para un reporte específico.
   * Endpoint: POST /api/reportes/{id}/orden-trabajo
   */
  crearOrdenTrabajo(idReporte: number, request: CrearOrdenTrabajoRequest): Observable<OrdenTrabajoResponse> {
    return this.http.post<OrdenTrabajoResponse>(`${this.apiUrl}/${idReporte}/orden-trabajo`, request);
  }

  /**
   * Obtiene la orden de trabajo asociada a un reporte.
   * Endpoint: GET /api/reportes/{id}/orden-trabajo
   */
  obtenerOrdenTrabajo(idReporte: number): Observable<OrdenTrabajoResponse> {
    return this.http.get<OrdenTrabajoResponse>(`${this.apiUrl}/${idReporte}/orden-trabajo`);
  }
}
