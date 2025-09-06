import "./footer.css";

export default function Footer(){
  return (
    <footer className="ftr">
      <div className="container ftr__grid">
        <div className="ftr__brand">
          <div className="ftr__logo" aria-hidden="true">✈</div>
          <h3>Aerolíneas</h3>
          <p>Conectamos destinos con seguridad y confort.</p>
        </div>

        <nav className="ftr__col" aria-label="Compañía">
          <h4>Compañía</h4>
          <a href="#">Nosotros</a>
          <a href="#">Trabaja con nosotros</a>
          <a href="#">Prensa</a>
        </nav>

        <nav className="ftr__col" aria-label="Ayuda">
          <h4>Ayuda</h4>
          <a href="#">Centro de soporte</a>
          <a href="#">Políticas y equipaje</a>
          <a href="#">Preguntas frecuentes</a>
        </nav>

        <div className="ftr__col">
          <h4>Contacto</h4>
          <p>+502 5555-5555</p>
          <p>soporte@aerolineas.com</p>
          <div className="ftr__social">
            <a href="#" aria-label="Facebook" title="Facebook">
              <svg width="20" height="20" viewBox="0 0 24 24"><path fill="currentColor" d="M13 22v-9h3l1-4h-4V6c0-1.1.9-2 2-2h2V0h-3c-3.3 0-6 2.7-6 6v3H5v4h3v9h5Z"/></svg>
            </a>
            <a href="#" aria-label="X" title="X">
              <svg width="20" height="20" viewBox="0 0 24 24"><path fill="currentColor" d="m3 3 7.5 9.2L3.5 21H7l5-6.1L16.5 21H21l-7.3-9 6.9-9h-3.5L12 8.9 7.4 3H3Z"/></svg>
            </a>
            <a href="#" aria-label="Instagram" title="Instagram">
              <svg width="20" height="20" viewBox="0 0 24 24"><path fill="currentColor" d="M7 2h10a5 5 0 0 1 5 5v10a5 5 0 0 1-5 5H7a5 5 0 0 1-5-5V7a5 5 0 0 1 5-5Zm10 2H7a3 3 0 0 0-3 3v10a3 3 0 0 0 3 3h10a3 3 0 0 0 3-3V7a3 3 0 0 0-3-3Zm-5 3a5 5 0 1 1 0 10 5 5 0 0 1 0-10Zm0 2.2A2.8 2.8 0 1 0 12 15.8 2.8 2.8 0 0 0 12 9.2ZM18 6.8a1 1 0 1 1-2 0a1 1 0 0 1 2 0Z"/></svg>
            </a>
          </div>
        </div>
      </div>

      <div className="ftr__bar">
        <div className="container ftr__bar__row">
          <small>© {new Date().getFullYear()} Aerolíneas. Todos los derechos reservados.</small>
          <div className="ftr__links">
            <a href="#">Privacidad</a>
            <span>•</span>
            <a href="#">Términos</a>
          </div>
        </div>
      </div>
    </footer>
  );
}
