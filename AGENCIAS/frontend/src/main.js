import { mount } from 'svelte';
import App from './App.svelte';
import './app.css';
import 'sweetalert2/dist/sweetalert2.min.css';

window.addEventListener('error', (e) => {
  console.error('[GLOBAL ERROR]', e.error || e.message, e);
});
window.addEventListener('unhandledrejection', (e) => {
  console.error('[UNHANDLED REJECTION]', e.reason || e);
});

console.log('[BOOT] main.js cargado, montando Svelte…');

export const app = mount(App, {
  target: document.getElementById('app'),
});
