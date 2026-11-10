import { Injectable, signal } from '@angular/core';

export type Tema = 'light' | 'dark';

@Injectable({
  providedIn: 'root'
})
export class TemaService {
  private readonly THEME_KEY = 'app-theme-preference';

  private timeoutTransicion?: ReturnType<typeof setTimeout>;

  /**
   * Signal reactivo que indica si el modo oscuro está activo
   */
  public esModoOscuro = signal<boolean>(false);

  constructor() {
    this.inicializarTema();
  }

  /**
   * Inicializa el tema leyendo localStorage o preferencia del sistema operativo
   */
  private inicializarTema(): void {
    if (typeof window === 'undefined') {
      return;
    }

    const temaGuardado = localStorage.getItem(this.THEME_KEY);
    const prefiereOscuro = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    const esOscuro = temaGuardado ? temaGuardado === 'dark' : prefiereOscuro;

    this.aplicarTema(esOscuro);
  }

  /**
   * Activa temporalmente la clase 'theme-transitioning' en el elemento raíz para
   * animar suavemente la interpolación de colores entre temas sin afectar el rendimiento
   */
  private activarTransicionSuave(): void {
    if (typeof document === 'undefined') {
      return;
    }

    document.documentElement.classList.add('theme-transitioning');
    if (this.timeoutTransicion) {
      clearTimeout(this.timeoutTransicion);
    }

    this.timeoutTransicion = setTimeout(() => {
      document.documentElement.classList.remove('theme-transitioning');
    }, 240);
  }

  /**
   * Alterna entre modo claro y modo oscuro con animación fluida
   */
  public alternarTema(): void {
    this.activarTransicionSuave();
    this.aplicarTema(!this.esModoOscuro());
  }

  /**
   * Aplica el tema en el DOM y persiste en localStorage
   */
  public aplicarTema(esOscuro: boolean): void {
    this.esModoOscuro.set(esOscuro);
    const temaNombre: Tema = esOscuro ? 'dark' : 'light';

    if (typeof document !== 'undefined') {
      document.documentElement.setAttribute('data-theme', temaNombre);
      if (esOscuro) {
        document.body.classList.add('dark-mode');
      } else {
        document.body.classList.remove('dark-mode');
      }
    }

    try {
      localStorage.setItem(this.THEME_KEY, temaNombre);
    } catch {
      // Ignorar restricciones en entornos con storage bloqueado
    }
  }
}
