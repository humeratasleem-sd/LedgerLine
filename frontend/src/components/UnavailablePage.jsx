import { ArrowUpRight, CircleDashed } from 'lucide-react'

function UnavailablePage({ eyebrow, title, description, icon: Icon }) {
  return (
    <section className="coming-page" aria-labelledby="coming-title">
      <div className="section-eyebrow"><span className="eyebrow-line" />{eyebrow}</div>
      <div className="coming-panel">
        <div className="coming-copy">
          <div className="coming-icon" aria-hidden="true"><Icon size={22} strokeWidth={1.7} /></div>
          <span className="coming-status"><CircleDashed size={14} aria-hidden="true" /> NOT AVAILABLE YET</span>
          <h1 id="coming-title">{title}</h1>
          <p>{description}</p>
          <div className="coming-footnote">
            <ArrowUpRight size={15} aria-hidden="true" />
            This area will appear when the relevant backend integration is available.
          </div>
        </div>
        <div className="coming-index" aria-hidden="true">LL<span> / </span>PREVIEW</div>
      </div>
    </section>
  )
}

export default UnavailablePage
