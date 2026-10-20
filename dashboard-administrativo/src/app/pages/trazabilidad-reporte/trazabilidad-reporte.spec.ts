import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TrazabilidadReporteComponent } from './trazabilidad-reporte';

describe('TrazabilidadReporteComponent', () => {
  let component: TrazabilidadReporteComponent;
  let fixture: ComponentFixture<TrazabilidadReporteComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TrazabilidadReporteComponent],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(TrazabilidadReporteComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
