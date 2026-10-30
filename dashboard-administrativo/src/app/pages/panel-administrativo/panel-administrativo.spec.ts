import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { PanelAdministrativoComponent } from './panel-administrativo';
import { ReporteService } from '../../core/services/reporte.service';

describe('PanelAdministrativoComponent', () => {
  let component: PanelAdministrativoComponent;
  let fixture: ComponentFixture<PanelAdministrativoComponent>;

  const reporteServiceMock = {
    obtenerCategorias: () => of([
      { nombre: 'Pavimentacion', conteo: 0 },
      { nombre: 'Alcantarillado', conteo: 0 },
      { nombre: 'Iluminacion', conteo: 0 }
    ]),
    obtenerTodos: () => of([]),
    obtenerPorCategoria: () => of([]),
    obtenerReportesFiltrados: () => of([]),
    obtenerMetricas: () => of({ totalReportes: 0, urgenciaAlta: 0, finalizados: 0 })
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PanelAdministrativoComponent],
      providers: [
        provideRouter([]),
        { provide: ReporteService, useValue: reporteServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PanelAdministrativoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load categories from ReporteService on init and select Todos', () => {
    expect(component.categorias.length).toBe(3);
    expect(component.categoriaSeleccionada).toBe('Todos');
  });

  it('should correctly identify "No iniciado" state', () => {
    expect(component.esNoIniciado('No iniciado')).toBe(true);
    expect(component.esNoIniciado('Pendiente')).toBe(true);
    expect(component.esNoIniciado('Iniciado')).toBe(false);
    expect(component.esNoIniciado('Finalizado')).toBe(false);
  });

  it('should correctly identify phases for subsequent states', () => {
    expect(component.obtenerTipoFase('Iniciado')).toBe('proceso');
    expect(component.obtenerTipoFase('En Proceso')).toBe('proceso');
    expect(component.obtenerTipoFase('En revisión')).toBe('revision');
    expect(component.obtenerTipoFase('Finalizado')).toBe('finalizado');
  });

  it('should return correct CSS classes for status pills', () => {
    expect(component.obtenerClaseEstado('Finalizado')).toBe('estado-finalizado');
    expect(component.obtenerClaseEstado('Iniciado')).toBe('estado-proceso');
    expect(component.obtenerClaseEstado('En revisión')).toBe('estado-revision');
    expect(component.obtenerClaseEstado('No iniciado')).toBe('estado-pendiente');
  });

  it('should return correct CSS classes for urgency pills', () => {
    expect(component.obtenerClaseUrgencia('Alta')).toBe('alta');
    expect(component.obtenerClaseUrgencia('Media')).toBe('media');
    expect(component.obtenerClaseUrgencia('Baja')).toBe('baja');
  });

  it('should initialize with default 60s auto-refresh and "hace unos momentos" relative time', () => {
    expect(component.intervaloSeleccionado).toBe(60);
    expect(component.tiempoRelativo).toBe('hace unos momentos');
  });

  it('should correctly format relative time in calcularTiempoRelativo', () => {
    const ahora = new Date();
    expect(component.calcularTiempoRelativo(ahora)).toBe('hace unos momentos');

    const hace1Min = new Date(ahora.getTime() - 65 * 1000);
    expect(component.calcularTiempoRelativo(hace1Min)).toBe('hace 1 min');

    const hace5Min = new Date(ahora.getTime() - 300 * 1000);
    expect(component.calcularTiempoRelativo(hace5Min)).toBe('hace 5 min');

    const hace1Hora = new Date(ahora.getTime() - 3650 * 1000);
    expect(component.calcularTiempoRelativo(hace1Hora)).toBe('hace 1 h');

    const hace3Horas = new Date(ahora.getTime() - 11000 * 1000);
    expect(component.calcularTiempoRelativo(hace3Horas)).toBe('hace 3 h');
  });

  it('should allow changing refresh interval to manual and custom seconds', () => {
    component.cambiarIntervalo(0);
    expect(component.intervaloSeleccionado).toBe(0);

    component.cambiarIntervalo(30);
    expect(component.intervaloSeleccionado).toBe(30);
  });

  it('should trigger manual refresh on actualizarManualmente', () => {
    const spy = vi.spyOn(component, 'refrescarDatos');
    component.actualizarManualmente();
    expect(spy).toHaveBeenCalledWith(true);
  });

  it('should initialize pagination with default page 1 and page size 10', () => {
    expect(component.paginaActual).toBe(1);
    expect(component.tamanoPagina).toBe(10);
    expect(component.opcionesTamanoPagina).toEqual([10, 20, 40]);
  });

  it('should paginate reports slice correctly according to tamanoPagina and paginaActual', () => {
    // Generar 25 reportes simulados
    component.reportes = Array.from({ length: 25 }, (_, i) => ({
      id: i + 1,
      codigo: `RPT-2026-000${i + 1}`,
      texto: `Reporte número ${i + 1}`,
      direccion: 'Calle Test 123',
      categoria: 'Pavimentacion',
      fechaIngreso: '2026-10-30T00:00:00',
      urgencia: 'Media',
      estado: 'No iniciado',
      sentimiento: 'Neutro',
      nivelRiesgo: 'MEDIO',
      huboAjuste: false
    }));

    expect(component.totalPaginas).toBe(3);
    expect(component.rangoInicio).toBe(1);
    expect(component.rangoFin).toBe(10);
    expect(component.reportesPaginados.length).toBe(10);
    expect(component.reportesPaginados[0].id).toBe(1);

    // Cambiar a página 2
    component.cambiarPagina(2);
    expect(component.paginaActual).toBe(2);
    expect(component.rangoInicio).toBe(11);
    expect(component.rangoFin).toBe(20);
    expect(component.reportesPaginados.length).toBe(10);
    expect(component.reportesPaginados[0].id).toBe(11);

    // Cambiar a página 3 (última con 5 elementos)
    component.cambiarPagina(3);
    expect(component.paginaActual).toBe(3);
    expect(component.rangoInicio).toBe(21);
    expect(component.rangoFin).toBe(25);
    expect(component.reportesPaginados.length).toBe(5);

    // Cambiar tamaño de página a 20 debe reiniciar a página 1
    component.cambiarTamanoPagina(20);
    expect(component.tamanoPagina).toBe(20);
    expect(component.paginaActual).toBe(1);
    expect(component.totalPaginas).toBe(2);
    expect(component.reportesPaginados.length).toBe(20);
  });
});
