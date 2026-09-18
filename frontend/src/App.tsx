import './App.css'

function App() {
  return (
    <div className="app-shell">
      <header className="site-header">
        <a className="brand" href="/" aria-label="Credit Lens home">
          <span className="brand-mark" aria-hidden="true">
            CL
          </span>
          <span>Credit Lens</span>
        </a>
        <span className="status-badge">Frontend foundation ready</span>
      </header>

      <main>
        <section className="hero" aria-labelledby="page-title">
          <p className="eyebrow">Positive Credit Register</p>
          <h1 id="page-title">A clearer view of every credit decision.</h1>
          <p className="hero-copy">
            Request a credit extract, review financing request history and
            understand the information behind a decision.
          </p>
        </section>

        <section className="next-steps" aria-labelledby="next-steps-title">
          <div className="section-heading">
            <div>
              <p className="eyebrow">Application scope</p>
              <h2 id="next-steps-title">What comes next</h2>
            </div>
          </div>

          <div className="feature-grid">
            <article className="feature-card">
              <span className="feature-number">01</span>
              <h3>Start a request</h3>
              <p>Submit a consumer identity code and select an extract purpose.</p>
            </article>
            <article className="feature-card">
              <span className="feature-number">02</span>
              <h3>Review history</h3>
              <p>Find completed and failed requests without exposing sensitive data.</p>
            </article>
            <article className="feature-card">
              <span className="feature-number">03</span>
              <h3>Inspect details</h3>
              <p>Open an extract to review its credit summary and loan information.</p>
            </article>
          </div>
        </section>
      </main>

      <footer>
        <span>Credit Lens</span>
        <span>React + TypeScript</span>
      </footer>
    </div>
  )
}

export default App
