import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { GestionarReporteComponent } from './gestionar-reporte';
import { ReporteService } from '../../core/services/reporte.service';

describe('GestionarReporteComponent', () => {
  let component: GestionarReporteComponent;
  let fixture: ComponentFixture<GestionarReporteComponent>;

  const reporteServiceMock = {
    obtenerReportePorId: () => of({
      id: 1,
      codigo: 'RPT-2026-0001',
      categoria: 'Pavimentacion',
      texto: 'Bache en la calle',
      direccion: 'Av. Libertad 123',
      fechaIngreso: '15/10/2026',
      urgencia: 'Alta',
      sentimiento: 'Enojado',
      estado: 'No Iniciado',
      nivelRiesgo: 'ALTO',
      huboAjuste: false
    }),
    obtenerOrdenTrabajo: () => of(null),
    crearOrdenTrabajo: () => of({})
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GestionarReporteComponent],
      providers: [
        provideRouter([]),
        { provide: ReporteService, useValue: reporteServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(GestionarReporteComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
