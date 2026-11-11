import {
  ArrowDownRight,
  ArrowRight,
  Check,
  Droplets,
  Lightbulb,
  MapPinned,
  MessageCircleMore,
  MoveUpRight,
  Recycle,
  Road,
  ShieldCheck,
  Waves,
  Zap,
} from 'lucide-react'
import { motion, useReducedMotion } from 'motion/react'
import { Link } from 'react-router-dom'
import { MotionPage } from '../components/MotionPage'

const categories = [
  { icon: Road, label: 'Roads & paths', detail: 'Potholes, damaged pavements, unsafe crossings', tone: 'category-coral' },
  { icon: Droplets, label: 'Water supply', detail: 'Leaks, interruptions, and water quality concerns', tone: 'category-blue' },
  { icon: Lightbulb, label: 'Electricity & lights', detail: 'Streetlights out and exposed electrical hazards', tone: 'category-amber' },
  { icon: Waves, label: 'Drainage & sewage', detail: 'Blocked drains, flooding, and sewage issues', tone: 'category-violet' },
  { icon: Recycle, label: 'Waste management', detail: 'Missed collections and overflowing bins', tone: 'category-green' },
]

const steps = [
  { number: '01', title: 'Spot something?', text: 'Notice an issue in your neighborhood that needs attention.' },
  { number: '02', title: 'Share the details', text: 'Tell us what is happening and where. A clear description helps.' },
  { number: '03', title: 'Follow the progress', text: 'Keep a view of the issue as it moves toward a resolution.' },
]

export function LandingPage() {
  const reduceMotion = useReducedMotion()

  return (
    <MotionPage>
      <section className="hero-section" id="main-content">
        <div className="container hero-grid">
          <div className="hero-copy">
            <div className="hero-kicker"><span className="status-dot" /> Your neighborhood, heard</div>
            <h1>Small reports.<br /><span>Meaningful change.</span></h1>
            <p className="hero-description">
              A simpler way to bring local issues into focus. Share what needs attention, then follow what happens next.
            </p>
            <div className="hero-actions">
              <Link className="button button-primary" to="/signin">
                Report a local issue <ArrowRight size={17} />
              </Link>
              <a className="button button-secondary" href="#how-it-works">
                See how it works <ArrowDownRight size={17} />
              </a>
            </div>
            <div className="hero-note">
              <span className="hero-note-mark"><MapPinned size={17} /></span>
              <span>A shared space for people who care about where they live.</span>
            </div>
          </div>
          <div className="hero-visual" aria-label="Illustration of a neighborhood map with a reported streetlight issue" role="img">
            <div className="map-card">
              <div className="map-header">
                <span className="map-live"><span className="status-dot" /> NEIGHBORHOOD PULSE</span>
                <span className="map-zoom">+ &nbsp;−</span>
              </div>
              <div className="map-canvas">
                <div className="map-block map-block-one" />
                <div className="map-block map-block-two" />
                <div className="map-block map-block-three" />
                <div className="map-block map-block-four" />
                <div className="map-road map-road-horizontal" />
                <div className="map-road map-road-vertical" />
                <div className="map-road map-road-diagonal" />
                <div className="map-park"><span>Public<br />space</span></div>
                <div className="map-street-label street-one">LOCAL STREET</div>
                <div className="map-street-label street-two">NEIGHBORHOOD ROAD</div>
                <div className="map-pin"><MapPinned size={15} /><span className="map-pin-pulse" /></div>
                <div className="map-you"><span /></div>
              </div>
              <div className="map-issue">
                <span className="issue-icon"><Lightbulb size={17} /></span>
                <span className="issue-copy"><strong>Issue location</strong><small>Pinpoint the place that needs attention</small></span>
                <span className="issue-status">Map pin</span>
              </div>
            </div>
            <div className="hero-float-card float-location"><MapPinned size={17} /><span>Right where it matters</span></div>
            <div className="hero-float-card float-update"><span className="update-check"><Check size={14} /></span><span>Community voices, connected</span></div>
          </div>
        </div>
        <a className="hero-scroll" href="#categories"><span /> Scroll to explore</a>
      </section>

      <section aria-labelledby="categories-title" className="section categories-section" id="categories">
        <div className="container">
          <div className="section-heading">
            <div><span className="eyebrow">A better neighborhood starts here</span><h2 id="categories-title">The everyday issues<br />worth speaking up about.</h2></div>
            <p>From the street outside to services we all rely on, make it easier to put local concerns on the map.</p>
          </div>
          <div className="category-grid">
            {categories.map(({ icon: Icon, label, detail, tone }, index) => (
              <motion.article
                className="category-card"
                initial={{ opacity: 0, y: reduceMotion ? 0 : 12 }}
                key={label}
                transition={{ delay: reduceMotion ? 0 : index * 0.055, duration: reduceMotion ? 0 : 0.28 }}
                whileHover={reduceMotion ? undefined : { y: -3, transition: { duration: 0.18 } }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, amount: 0.2 }}
              >
                <span className={`category-icon ${tone}`}><Icon size={20} strokeWidth={1.9} /></span>
                <h3>{label}</h3>
                <p>{detail}</p>
                <MoveUpRight aria-hidden="true" className="category-arrow" size={17} />
              </motion.article>
            ))}
          </div>
        </div>
      </section>

      <section aria-labelledby="how-title" className="section how-section" id="how-it-works">
        <div className="container how-grid">
          <div className="how-intro">
            <span className="eyebrow">Simple by design</span>
            <h2 id="how-title">From noticing<br />to knowing.</h2>
            <p>Good local change begins with a clear picture of what needs attention. We make the first step easier.</p>
            <Link className="text-link" to="/signin">Get started <ArrowRight size={16} /></Link>
          </div>
          <div className="steps-list">
            {steps.map((step) => (
              <div className="step-row" key={step.number}>
                <span className="step-number">{step.number}</span>
                <div><h3>{step.title}</h3><p>{step.text}</p></div>
                <span aria-hidden="true" className="step-line" />
              </div>
            ))}
          </div>
        </div>
      </section>

      <section aria-labelledby="features-title" className="feature-section">
        <div className="container feature-grid">
          <div className="feature-copy">
            <span className="eyebrow eyebrow-light">Made for everyday civic life</span>
            <h2 id="features-title">A little more clarity.<br />A lot more connection.</h2>
            <p>One thoughtful place to raise a concern, keep track, and help your community stay in the loop.</p>
            <ul className="feature-list">
              <li><span><Check size={15} /></span>Keep reports organized in one place</li>
              <li><span><Check size={15} /></span>Know where an issue stands over time</li>
              <li><span><Check size={15} /></span>Make local patterns easier to understand</li>
            </ul>
            <Link className="button button-light" to="/signin">Join the conversation <ArrowRight size={16} /></Link>
          </div>
          <div className="feature-visual" aria-hidden="true">
            <div className="feature-orbit orbit-one" /><div className="feature-orbit orbit-two" />
            <div className="feature-center"><MessageCircleMore size={34} strokeWidth={1.6} /></div>
            <div className="feature-node node-top"><ShieldCheck size={18} /><span>Clear updates</span></div>
            <div className="feature-node node-right"><MapPinned size={18} /><span>Local context</span></div>
            <div className="feature-node node-bottom"><Zap size={18} /><span>Shared momentum</span></div>
            <div className="feature-node node-left"><Droplets size={18} /><span>Real concerns</span></div>
          </div>
        </div>
      </section>

      <section className="closing-section">
        <div className="container closing-inner">
          <div><span className="eyebrow">Change starts close to home</span><h2>What needs attention<br />on your street?</h2></div>
          <Link className="button button-primary" to="/signin">Share what you see <ArrowRight size={17} /></Link>
        </div>
      </section>
    </MotionPage>
  )
}
