import React, { useState, useEffect } from 'react';
import { courseData } from '../data/courses';
import { courseAPI } from '../services/api';

export default function RecommendedCourses({ onOpenCourse }) {
  const [activeFilter, setActiveFilter] = useState('all');
  const [backendCourses, setBackendCourses] = useState([]);
  const [isLiveBackend, setIsLiveBackend] = useState(false);
  const [loading, setLoading] = useState(false);

  const fetchCourses = () => {
    setLoading(true);
    courseAPI
      .getAll()
      .then((res) => {
        if (Array.isArray(res.data) && res.data.length > 0) {
          setBackendCourses(res.data);
          setIsLiveBackend(true);
        }
      })
      .catch(() => {
        setIsLiveBackend(false);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchCourses();
  }, []);

  // Map backend database courses to rich UI cards
  const liveCards = backendCourses.map((c, index) => {
    const icons = ['terminal', 'database', 'code', 'hub', 'layers', 'data_object'];
    const bgs = ['bg-emerald-600', 'bg-blue-600', 'bg-purple-600', 'bg-amber-600'];
    const lightBgs = ['bg-emerald-100 text-emerald-700', 'bg-blue-100 text-blue-700', 'bg-purple-100 text-purple-700'];

    return {
      id: c.id,
      isDbCourse: true,
      title: c.title,
      badge: 'Database Live',
      badgeBg: bgs[index % bgs.length],
      category: 'Verified Course',
      filterCategory: 'dev',
      rating: c.averageRating || '4.9',
      reviews: `(${c.totalReviews || 18})`,
      lessons: `${c.totalLectures || 5} Lessons`,
      price: 'Free',
      icon: icons[index % icons.length],
      iconBg: lightBgs[index % lightBgs.length],
      instructor: {
        name: c.instructorName || 'Lead Mentor',
        role: 'Verified Instructor'
      },
      desc: c.description
    };
  });

  const curatedCourses = Object.values(courseData);
  // Merge live database courses with curated template tracks
  const combinedCourses = [...liveCards, ...curatedCourses];

  const filteredCourses = activeFilter === 'all'
    ? combinedCourses
    : combinedCourses.filter((c) => c.filterCategory === activeFilter);

  return (
    <section className="w-full bg-surface py-20 lg:py-28" id="featured-courses">
      <div className="max-w-7xl mx-auto px-6 lg:px-12 flex flex-col gap-12">
        {/* Section Heading & Filter Tabs */}
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
          <div className="flex flex-col gap-3 max-w-xl">
            <div className="flex items-center gap-2">
              <span className="font-label-sm text-label-sm text-primary uppercase tracking-widest font-bold">
                Curated Curriculum
              </span>
              <span
                className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold ${
                  isLiveBackend
                    ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                    : 'bg-amber-50 text-amber-700 border border-amber-200'
                }`}
                title="Status of Spring Boot Backend API connection"
              >
                <span
                  className={`w-1.5 h-1.5 rounded-full ${
                    isLiveBackend ? 'bg-emerald-500 animate-pulse' : 'bg-amber-500'
                  }`}
                />
                {isLiveBackend ? `API Connected (${backendCourses.length} from DB)` : 'Curated Fallback'}
              </span>
            </div>
            <h2 className="font-headline-xl text-headline-xl text-on-surface">
              Recommended Courses &amp; Tracks
            </h2>
            <p className="font-body-md text-body-md text-on-surface-variant">
              Explore world-class, production-ready courses curated by industry leads from Webflow, Figma, and Silicon Valley startups.
            </p>
          </div>

          {/* Filter Pills */}
          <div className="flex items-center gap-2 p-1.5 bg-surface-container-low rounded-full border border-surface-container-highest">
            <button
              onClick={() => setActiveFilter('all')}
              className={`px-4 py-2 rounded-full font-label-md text-label-md transition-all duration-200 cursor-pointer ${
                activeFilter === 'all'
                  ? 'bg-primary-container text-on-primary shadow-sm'
                  : 'text-on-surface-variant hover:text-on-surface'
              }`}
            >
              All Tracks ({combinedCourses.length})
            </button>
            <button
              onClick={() => setActiveFilter('design')}
              className={`px-4 py-2 rounded-full font-label-md text-label-md transition-all duration-200 cursor-pointer ${
                activeFilter === 'design'
                  ? 'bg-primary-container text-on-primary shadow-sm'
                  : 'text-on-surface-variant hover:text-on-surface'
              }`}
            >
              UI/UX Design
            </button>
            <button
              onClick={() => setActiveFilter('dev')}
              className={`px-4 py-2 rounded-full font-label-md text-label-md transition-all duration-200 cursor-pointer ${
                activeFilter === 'dev'
                  ? 'bg-primary-container text-on-primary shadow-sm'
                  : 'text-on-surface-variant hover:text-on-surface'
              }`}
            >
              Web &amp; Software Dev
            </button>
          </div>
        </div>

        {/* Course Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {filteredCourses.map((c, i) => (
            <div
              key={`${c.id}-${i}`}
              onClick={() => onOpenCourse(c.id)}
              className="group bg-surface-container-lowest rounded-2xl overflow-hidden shadow-sm hover:shadow-2xl hover:-translate-y-2 border border-transparent hover:border-primary-fixed transition-all duration-300 flex flex-col cursor-pointer relative"
              style={{ transitionDelay: `${i * 50}ms` }}
            >
              {/* Card Thumbnail / Header */}
              <div className="relative w-full aspect-video bg-surface-container flex items-center justify-center overflow-hidden">
                <div
                  className={`absolute top-3 left-3 px-2.5 py-1 rounded-full text-white font-label-sm text-label-sm font-bold shadow-sm z-10 ${c.badgeBg}`}
                >
                  {c.badge}
                </div>
                <div
                  className={`w-14 h-14 rounded-2xl flex items-center justify-center shadow-sm group-hover:scale-110 group-hover:rotate-3 transition-transform duration-300 ${c.iconBg}`}
                >
                  <span className="material-symbols-outlined text-[32px]">
                    {c.icon}
                  </span>
                </div>
                <div className="absolute bottom-2 right-2 px-2 py-0.5 rounded bg-black/60 text-white font-label-sm text-label-sm backdrop-blur-xs">
                  {c.lessons}
                </div>

                {/* Quick Preview Hover Overlay */}
                <div className="absolute inset-0 bg-primary/20 backdrop-blur-[2px] opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center z-20">
                  <span className="px-4 py-2 rounded-full bg-surface-container-lowest text-primary font-label-sm font-bold shadow-lg flex items-center gap-1.5 transform translate-y-2 group-hover:translate-y-0 transition-transform">
                    <span className="material-symbols-outlined text-[16px]">visibility</span> Quick Preview
                  </span>
                </div>
              </div>

              {/* Card Body */}
              <div className="p-5 flex flex-col flex-1 gap-3">
                <span className="font-label-sm text-label-sm text-primary font-bold uppercase tracking-wider">
                  {c.category}
                </span>
                <h3 className="font-headline-md text-headline-md text-on-surface font-bold leading-snug group-hover:text-primary transition-colors line-clamp-2">
                  {c.title}
                </h3>
                <p className="font-body-sm text-body-sm text-on-surface-variant truncate">
                  {c.instructor?.name} • {c.instructor?.role ? c.instructor.role.split('•')[0] : 'Instructor'}
                </p>
                <div className="mt-auto pt-3 border-t border-surface-container flex items-center justify-between">
                  <div className="flex items-center gap-1 text-secondary-container">
                    <span
                      className="material-symbols-outlined text-[16px]"
                      style={{ fontVariationSettings: "'FILL' 1" }}
                    >
                      star
                    </span>
                    <span className="font-label-md text-label-md text-on-surface font-bold">
                      {c.rating}
                    </span>
                    <span className="font-label-sm text-label-sm text-on-surface-variant">
                      {c.reviews}
                    </span>
                  </div>
                  <span className="font-headline-sm text-headline-sm text-on-surface font-bold group-hover:text-primary transition-colors">
                    {c.price}
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
