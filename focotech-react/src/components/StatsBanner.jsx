import React, { useState, useEffect, useRef } from 'react';

function AnimatedCounter({ target, suffix = '', isDecimal = false, shouldStart = false }) {
  const [value, setValue] = useState(0);

  useEffect(() => {
    if (!shouldStart) return;

    let startTime = null;
    let animationFrameId;
    const duration = 1600; // 1.6s duration for smooth tick-up

    const step = (timestamp) => {
      if (!startTime) startTime = timestamp;
      const elapsed = timestamp - startTime;
      const progress = Math.min(elapsed / duration, 1);

      // Smooth ease-out cubic easing
      const easeOut = 1 - Math.pow(1 - progress, 3);
      const current = easeOut * target;

      if (isDecimal) {
        setValue(parseFloat(current.toFixed(1)));
      } else {
        setValue(Math.floor(current));
      }

      if (progress < 1) {
        animationFrameId = requestAnimationFrame(step);
      } else {
        setValue(target);
      }
    };

    animationFrameId = requestAnimationFrame(step);
    return () => cancelAnimationFrame(animationFrameId);
  }, [target, isDecimal, shouldStart]);

  return (
    <span>
      {isDecimal ? value.toFixed(1) : value.toLocaleString()} {suffix}
    </span>
  );
}

export default function StatsBanner() {
  const [hasAnimated, setHasAnimated] = useState(false);
  const containerRef = useRef(null);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setHasAnimated(true);
          observer.disconnect();
        }
      },
      { threshold: 0.2 }
    );

    if (containerRef.current) {
      observer.observe(containerRef.current);
    }

    return () => observer.disconnect();
  }, []);

  const stats = [
    {
      label: 'Ongoing Courses',
      target: 1830,
      suffix: '+',
      isDecimal: false
    },
    {
      label: 'Ongoing Enrollments',
      target: 51.8,
      suffix: 'lakh+',
      isDecimal: true
    },
    {
      label: 'Exam Registration',
      target: 15.2,
      suffix: 'lakh+',
      isDecimal: true
    }
  ];

  return (
    <section
      ref={containerRef}
      className="w-full bg-surface-container-low/70 backdrop-blur-md border-y border-outline-variant/25 py-8 sm:py-10"
    >
      <div className="max-w-7xl mx-auto px-6 lg:px-16">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-8 sm:gap-6 items-center text-center sm:text-left">
          {stats.map((stat, index) => (
            <div
              key={stat.label}
              className={`flex flex-col gap-1.5 transition-transform duration-500 hover:scale-105 cursor-default ${
                index === 1 ? 'sm:text-center' : index === 2 ? 'sm:text-right' : 'sm:text-left'
              }`}
            >
              <span className="text-on-surface-variant font-medium text-sm sm:text-base">
                {stat.label}
              </span>
              <span className="bg-gradient-to-r from-primary to-primary-container bg-clip-text text-transparent font-headline-xl text-2xl sm:text-3xl lg:text-4xl font-extrabold tracking-tight">
                <AnimatedCounter
                  target={stat.target}
                  suffix={stat.suffix}
                  isDecimal={stat.isDecimal}
                  shouldStart={hasAnimated}
                />
              </span>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
