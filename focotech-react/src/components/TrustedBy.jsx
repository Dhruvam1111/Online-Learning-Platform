import React from 'react';

export default function TrustedBy() {
  const brands = [
    { name: 'Figma', tracking: 'tracking-tighter' },
    { name: 'webflow', tracking: 'tracking-tight' },
    { name: 'stripe', tracking: 'tracking-tighter' },
    { name: '▲ Vercel', tracking: 'tracking-tight' },
    { name: 'Linear', tracking: 'tracking-wider' },
    { name: 'Spotify', tracking: 'tracking-tight' }
  ];

  return (
    <section className="w-full bg-surface-container-low py-10 overflow-hidden">
      <div className="max-w-7xl mx-auto px-6 lg:px-12 flex flex-col md:flex-row items-center justify-between gap-6">
        <p className="font-label-md text-label-md text-on-surface-variant uppercase tracking-wider font-bold shrink-0">
          Trusted by engineers &amp; designers at:
        </p>
        <div className="flex flex-wrap items-center justify-between w-full md:w-auto gap-8 md:gap-12 opacity-70 hover:opacity-100 transition-opacity duration-300">
          {brands.map((b) => (
            <span
              key={b.name}
              className={`font-headline-md text-headline-md font-bold ${b.tracking} text-on-surface/80 hover:text-primary hover:scale-110 transition-all duration-200 cursor-pointer`}
            >
              {b.name}
            </span>
          ))}
        </div>
      </div>
    </section>
  );
}
