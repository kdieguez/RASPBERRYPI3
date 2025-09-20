<script>
  import { navigate } from '../lib/router';
</script>

<footer class="footer">
  <div class="wrap">
    <div>© {new Date().getFullYear()} Agencia · Hecho con ✈️</div>
    <nav style="display:flex; gap:10px">
      <a href="#/" class="link" on:click|preventDefault={() => navigate('/')}>Inicio</a>
      <a href="#/register" class="link" on:click|preventDefault={() => navigate('/register')}>Registrarme</a>
    </nav>
  </div>
</footer>
