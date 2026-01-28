---
theme: seriph
background: /images/EPFL.jpg
title: PrintWizard
info: |
  ## PrintWizard Master's thesis
class: cover-epfl text-white text-center
drawings:
  persist: false
transition: slide-left
mdc: true
duration: 40min
layout: cover
---

# PrintWizard:
# Next-Level Trace-Based Debugging

<div class="cover-meta">
  <div class="cover-meta__top">Master’s Thesis Presentation · SYSTEMF</div>
  <div class="cover-meta__date">29.01.2026</div>

  <div class="cover-meta__author">Kelvin Kappeler</div>
  <div class="cover-meta__others">Shardul Chiplunkar · Clément Pit-Claudel</div>
</div>

<style>
.cover-meta{
  margin-top: 3rem;
  display: inline-block;
  padding: 1.3rem 2.1rem;
  border-radius: 18px;
  background: rgba(0,0,0,0.55);
  backdrop-filter: blur(6px);
  box-shadow: 0 12px 35px rgba(0,0,0,0.35);
  text-align: center;
}

.cover-meta__top{
  font-size: 1.25rem;
  font-weight: 600;
  line-height: 1.25;
  opacity: 0.98;
}

.cover-meta__date{
  margin-top: 0.25rem;
  font-size: 1.1rem;
  font-weight: 500;
  opacity: 0.95;
}

.cover-meta__author{
  margin-top: 0.95rem;
  font-size: 1.55rem;
  font-weight: 700;
  line-height: 1.15;
}

.cover-meta__others{
  margin-top: 0.35rem;
  font-size: 1.2rem;
  font-weight: 550;
  opacity: 0.98;
}

.slidev-layout.cover-epfl {
  position: relative;
}
.slidev-layout.cover-epfl::before {
  content: "";
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.45);
  backdrop-filter: blur(1.5px);
}
.slidev-layout.cover-epfl > * {
  position: relative;
  z-index: 1;
}

.slidev-layout.cover-epfl h1,
.slidev-layout.cover-epfl p {
  text-shadow: 0 2px 10px rgba(0,0,0,0.55);
}

.slidev-layout.cover-epfl h1 { font-size: 3.2rem; line-height: 1.05; }
.slidev-layout.cover-epfl h2 { font-size: 2.0rem; opacity: 0.95; margin-top: 0.6rem; }
</style>

<!--
Good luck!!!!
-->

---
src: ./pages/introduction.md
---
---
src: ./pages/architecture.md
---