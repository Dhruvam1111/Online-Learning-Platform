import React from 'react';

export default function Testimonials() {
  const testimonials = [
    {
      quote: '"The Design System course with Scott Warden completely demystified atomic tokens and multi-brand theming. I landed a Senior Product Designer role at Linear three weeks after finishing my capstone."',
      name: 'Kayla Lin',
      role: 'Product Designer @ Linear',
      avatar: 'KL',
      avatarBg: 'bg-primary-fixed text-primary',
      borderHover: 'hover:border-primary-fixed/60'
    },
    {
      quote: '"The daily streak system looks simple, but it fundamentally changed my learning discipline. 54 days in, and I went from struggling with CSS flexbox to building accessible React interfaces from scratch."',
      name: 'David Miller',
      role: 'Frontend Developer @ Vercel',
      avatar: 'DM',
      avatarBg: 'bg-secondary-fixed text-secondary',
      borderHover: 'hover:border-secondary-fixed/60'
    },
    {
      quote: '"Having actual video reviews from staff engineers on my Webflow client deliverables gave me the confidence to charge $8k per project. Focotech is worth 100x its price."',
      name: 'Sara Al-Mansoor',
      role: 'Freelance Design Director',
      avatar: 'SA',
      avatarBg: 'bg-tertiary-fixed text-tertiary',
      borderHover: 'hover:border-tertiary-fixed/60'
    }
  ];

  return (
    <section id="testimonials" className="w-full bg-surface-container-low py-20 lg:py-28">
      <div className="max-w-7xl mx-auto px-6 lg:px-12 flex flex-col gap-12">
        <div className="flex flex-col items-center text-center max-w-2xl mx-auto gap-4">
          <span className="font-label-sm text-label-sm text-primary uppercase tracking-widest font-bold">
            Student Success
          </span>
          <h2 className="font-headline-xl text-headline-xl text-on-surface">
            From First Milestone to Dream Job Offer
          </h2>
          <p className="font-body-md text-body-md text-on-surface-variant">
            Hear how designers and engineers accelerated their careers by staying consistent with Focotech streaks and project tracks.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {testimonials.map((t, i) => (
            <div
              key={t.name}
              className={`group bg-surface-container-lowest p-6 rounded-2xl shadow-sm hover:shadow-xl hover:-translate-y-2 border border-transparent ${t.borderHover} transition-all duration-300 flex flex-col gap-4`}
              style={{ transitionDelay: `${i * 120}ms` }}
            >
              <div className="flex items-center gap-1 text-secondary-container">
                {[...Array(5)].map((_, idx) => (
                  <span
                    key={idx}
                    className="material-symbols-outlined text-[18px] group-hover:scale-110 transition-transform"
                    style={{ fontVariationSettings: "'FILL' 1" }}
                  >
                    star
                  </span>
                ))}
              </div>
              <p className="font-body-md text-body-md text-on-surface leading-relaxed">
                {t.quote}
              </p>
              <div className="mt-auto pt-4 flex items-center gap-3 border-t border-surface-container">
                <div
                  className={`w-10 h-10 rounded-full flex items-center justify-center font-bold group-hover:scale-110 transition-transform ${t.avatarBg}`}
                >
                  {t.avatar}
                </div>
                <div>
                  <h4 className="font-headline-sm text-headline-sm text-on-surface font-bold">
                    {t.name}
                  </h4>
                  <span className="font-body-sm text-body-sm text-on-surface-variant">
                    {t.role}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
