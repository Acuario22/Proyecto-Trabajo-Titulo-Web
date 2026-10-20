import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
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
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load categories from ReporteService on init and select Todos', () => {
    expect(component.categorias.length).toBe(3);
    expect(component.categoriaSeleccionada).toBe('Todos');
  });
});
