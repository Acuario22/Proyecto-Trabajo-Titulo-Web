import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { TemaService } from '../../../core/services/tema.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './header.html',
  styleUrl: './header.css'
})
export class HeaderComponent {
  private router = inject(Router);
  public temaService = inject(TemaService);

  @Input() titulo: string = 'Municipalidad · Plataforma Urbana';
  @Input() subtitulo: string = 'Gestión de Reportes Urbanos';
  @Input() mostrarVolver: boolean = false;
  @Input() textoVolver: string = 'Volver al panel';
  @Input() rutaVolver: string = '/';
  @Input() mostrarEstado: boolean = false;
  @Input() textoEstado: string = 'Sistema activo';
  @Input() perfilTexto: string = 'Perfil administrador';
  @Input() mostrarPerfil: boolean = true;
  @Input() mostrarIconoMunicipalidad: boolean = true;
  @Input() mostrarSelectorTema: boolean = true;

  @Output() volver = new EventEmitter<void>();

  onVolver(): void {
    if (this.volver.observed) {
      this.volver.emit();
    } else {
      this.router.navigate([this.rutaVolver]);
    }
  }

  alternarTema(): void {
    this.temaService.alternarTema();
  }
}

