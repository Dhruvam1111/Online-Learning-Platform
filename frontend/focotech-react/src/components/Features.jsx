import React from 'react';

export default function Features() {
  const features = [
    {
      icon: 'terminal',
      title: 'Project-Based Learning',
      desc: 'Construct end-to-end design token frameworks, responsive Webflow sites, and full-stack React web apps built to ship.',
      cta: '40+ Real Case Briefs →',
      iconBg: 'bg-primary-fixed text-primary group-hover:bg-primary-container group-hover:text-on-primary',
      borderHover: 'hover:border-primary-fixed',
      titleHover: 'group-hover:text-primary',
      ctaColor: 'text-primary'
    },
    {
      icon: 'local_fire_department',
      title: 'Gamified Streaks',
      desc: 'Build unstoppable momentum. Our daily micro-challenges and milestone analytics guarantee you study with joy every day.',
      cta: 'Explore Streak Engine →',
      iconBg: 'bg-secondary-fixed text-secondary group-hover:bg-secondary-container group-hover:text-on-secondary',
      borderHover: 'hover:border-secondary-fixed',
      titleHover: 'group-hover:text-secondary',
      ctaColor: 'text-secondary'
    },
    {
      icon: 'record_voice_over',
      title: 'Industry Mentors',
      desc: 'Receive detailed asynchronous Loom video critiques on your Figma components and pull requests from Staff designers.',
      cta: 'Meet Instructors →',
      iconBg: 'bg-tertiary-fixed text-tertiary group-hover:bg-tertiary group-hover:text-on-tertiary',
      borderHover: 'hover:border-tertiary-fixed',
      titleHover: 'group-hover:text-tertiary',
      ctaColor: 'text-tertiary'
    },
    {
      icon: 'verified',
      title: 'Verified Credentials',
      desc: 'Earn verifiable, cryptographic badges and certificates of completion vetted and indexed by leading recruiting partners.',
      cta: 'View Credentials →',
      iconBg: 'bg-surface-container-high text-primary-container group-hover:bg-primary-container group-hover:text-on-primary',
      borderHover: 'hover:border-outline-variant',
      titleHover: 'group-hover:text-primary',
      ctaColor: 'text-primary-container'
    }
  ];

  return (
    <section id="features" className="w-full bg-surface py-20 lg:py-28">
      <div className="max-w-7xl mx-auto px-6 lg:px-12 flex flex-col gap-14">
        {/* Section Heading */}
        <div className="flex flex-col items-center text-center max-w-3xl mx-auto gap-4">
          <span className="font-label-sm text-label-sm text-primary uppercase tracking-widest font-bold">
            The Focotech Difference
          </span>
          <h2 className="font-headline-xl text-headline-xl text-on-surface">
            Education Engineered for Real-World Mastery
          </h2>
          <p className="font-body-md text-body-md text-on-surface-variant">
            Say goodbye to passive video tutorials. Our platform pairs interactive sandboxes, production system architectures, and daily momentum triggers.
          </p>
        </div>

        {/* Bento Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {features.map((f, i) => (
            <div
              key={f.title}
              className={`group flex flex-col bg-surface-container-lowest p-7 rounded-2xl shadow-sm hover:shadow-xl hover:-translate-y-2 border border-transparent ${f.borderHover} transition-all duration-300 gap-4 cursor-pointer`}
              style={{ transitionDelay: `${i * 100}ms` }}
            >
              <div
                className={`w-12 h-12 rounded-2xl flex items-center justify-center transition-all duration-300 group-hover:scale-110 ${f.iconBg}`}
              >
                <span className="material-symbols-outlined text-[26px]">
                  {f.icon}
                </span>
              </div>
              <h3 className={`font-headline-md text-headline-md text-on-surface ${f.titleHover} transition-colors`}>
                {f.title}
              </h3>
              <p className="font-body-sm text-body-sm text-on-surface-variant leading-relaxed">
                {f.desc}
              </p>
              <div className="mt-auto pt-2">
                <span className={`font-label-sm text-label-sm ${f.ctaColor} font-bold group-hover:translate-x-1.5 inline-flex items-center gap-1 transition-transform`}>
                  {f.cta}
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
