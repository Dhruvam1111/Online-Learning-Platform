import React, { useState, useEffect } from 'react';
import Header from './components/Header';
import HeroSection from './components/HeroSection';
import StatsBanner from './components/StatsBanner';
import Features from './components/Features';
import DashboardPreview from './components/DashboardPreview';
import RecommendedCourses from './components/RecommendedCourses';
import Testimonials from './components/Testimonials';
import CtaBanner from './components/CtaBanner';
import Footer from './components/Footer';
import CourseModal from './components/CourseModal';
import SignInPage from './components/SignInPage';

export default function App() {
  const [currentPage, setCurrentPage] = useState(() => {
    return window.location.hash === '#signin' ? 'signin' : 'home';
  });
  const [modalCourseId, setModalCourseId] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  useEffect(() => {
    const handleHashChange = () => {
      if (window.location.hash === '#signin') {
        setCurrentPage('signin');
      } else {
        setCurrentPage('home');
      }
    };

    window.addEventListener('hashchange', handleHashChange);
    return () => window.removeEventListener('hashchange', handleHashChange);
  }, []);

  const navigateTo = (page) => {
    setCurrentPage(page);
    if (page === 'signin') {
      window.location.hash = 'signin';
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } else {
      window.location.hash = '';
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  const handleOpenCourse = (courseId) => {
    setModalCourseId(courseId);
    setIsModalOpen(true);
  };

  const handleCloseCourse = () => {
    setIsModalOpen(false);
  };

  const handleExploreCourses = () => {
    const section = document.getElementById('featured-courses');
    if (section) {
      section.scrollIntoView({ behavior: 'smooth' });
    }
  };

  if (currentPage === 'signin') {
    return <SignInPage onNavigateHome={() => navigateTo('home')} />;
  }

  return (
    <div className="min-h-screen bg-surface font-body-md text-on-surface antialiased selection:bg-primary-container selection:text-on-primary">
      {/* Top Header Navigation */}
      <Header
        onNavigateSignIn={() => navigateTo('signin')}
        onNavigateSignUp={() => navigateTo('signin')}
      />

      {/* Main Page Canvas */}
      <main className="w-full pt-20 bg-surface">
        <div className="flex flex-col w-full">
          {/* Hero Section with Redesigned Target UI (Image 2) */}
          <HeroSection onExploreCourses={handleExploreCourses} />

          {/* Stats Metrics Banner */}
          <StatsBanner />

          {/* Bento Grid Feature Highlights */}
          <Features />

          {/* Interactive Student Dashboard Sneak-Peek */}
          <DashboardPreview onOpenCourse={handleOpenCourse} />

          {/* Recommended Courses Grid with Filter Tabs */}
          <RecommendedCourses onOpenCourse={handleOpenCourse} />

          {/* Student Success Reviews */}
          <Testimonials />

          {/* High-Impact CTA Banner */}
          <CtaBanner />
        </div>
      </main>

      {/* Footer */}
      <Footer />

      {/* Interactive Course Details Modal Dialog */}
      <CourseModal
        courseId={modalCourseId}
        isOpen={isModalOpen}
        onClose={handleCloseCourse}
      />
    </div>
  );
}
