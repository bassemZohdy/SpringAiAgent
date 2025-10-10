import { Injectable } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

export type MarkdownRenderer = typeof import('marked')['marked'];

@Injectable({
  providedIn: 'root'
})
export class MarkdownService {
  private renderer?: MarkdownRenderer;
  private loadingPromise?: Promise<void>;

  constructor(private sanitizer: DomSanitizer) {}

  preload(): Promise<void> {
    return this.loadRenderer();
  }

  renderSync(content: string): SafeHtml {
    if (this.renderer) {
      return this.renderWithRenderer(this.renderer, content);
    }

    void this.loadRenderer();
    return this.sanitizer.bypassSecurityTrustHtml(this.escapeHtml(content));
  }

  async render(content: string): Promise<SafeHtml> {
    await this.loadRenderer();

    if (this.renderer) {
      return this.renderWithRenderer(this.renderer, content);
    }

    return this.sanitizer.bypassSecurityTrustHtml(this.escapeHtml(content));
  }

  private loadRenderer(): Promise<void> {
    if (!this.loadingPromise) {
      this.loadingPromise = import('marked')
        .then(({ marked }) => {
          marked.setOptions({
            breaks: true,
            gfm: true
          });
          this.renderer = marked;
        })
        .catch(error => {
          console.error('Failed to load markdown renderer', error);
          this.renderer = undefined;
        });
    }

    return this.loadingPromise;
  }

  private renderWithRenderer(renderer: MarkdownRenderer, content: string): SafeHtml {
    const parsed = typeof renderer.parse === 'function'
      ? renderer.parse(content)
      : renderer(content as unknown as Parameters<MarkdownRenderer>[0]);

    if (parsed && typeof (parsed as PromiseLike<string>).then === 'function') {
      // Fall back to escaped HTML if marked is configured for async parsing
      return this.sanitizer.bypassSecurityTrustHtml(this.escapeHtml(content));
    }

    return this.sanitizer.bypassSecurityTrustHtml(parsed as string);
  }

  private escapeHtml(value: string): string {
    return value
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;')
      .replace(/\n/g, '<br/>');
  }
}
