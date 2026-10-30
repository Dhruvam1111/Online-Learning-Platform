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
import StudentDashboard from './components/StudentDashboard';
import InstructorDashboard from './components/InstructorDashboard';
import AdminDashboard from './components/AdminDashboard';
import { useAuth } from './context/AuthContext';

export default function App() {
  const { isAuthenticated, user, logout } = useAuth();
  const [currentPage, setCurrentPage] = useState(() => {
    if (window.location.hash === '#signin') return 'signin';
    if (window.location.hash === '#dashboard') return 'dashboard';
    return 'home';
  });
  const [modalCourseId, setModalCourseId] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  useEffect(() => {
    const handleHashChange = () => {
      if (window.location.hash === '#signin') {
        setCurrentPage('signin');
      } else if (window.location.hash === '#dashboard') {
        setCurrentPage('dashboard');
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
    } else if (page === 'dashboard') {
      window.location.hash = 'dashboard';
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

  // View 1: Sign In Page
  if (currentPage === 'signin') {
    return (
      <SignInPage
        onNavigateHome={() => navigateTo('home')}
        onNavigateDashboard={() => navigateTo('dashboard')}
      />
    );
  }

  // View 2: Role-Aware Dashboard (Student, Instructor, Admin)
  if (currentPage === 'dashboard') {
    if (!isAuthenticated) {
      return (
        <SignInPage
          onNavigateHome={() => navigateTo('home')}
          onNavigateDashboard={() => navigateTo('dashboard')}
          initialNotice="Please sign in to access your portal."
        />
      );
    }
    if (user?.role === 'instructor') {
      return <InstructorDashboard onNavigateHome={() => navigateTo('home')} />;
    }
    if (user?.role === 'admin') {
      return <AdminDashboard onNavigateHome={() => navigateTo('home')} />;
    }
    return <StudentDashboard onNavigateHome={() => navigateTo('home')} />;
  }

  // View 3: Landing Page
  return (
    <div className="min-h-screen bg-surface font-body-md text-on-surface antialiased selection:bg-primary-container selection:text-on-primary">
      {/* Top Header Navigation */}
      <Header
        user={user}
        isAuthenticated={isAuthenticated}
        onLogout={logout}
        onNavigateSignIn={() => navigateTo('signin')}
        onNavigateSignUp={() => navigateTo('signin')}
        onNavigateDashboard={() => navigateTo('dashboard')}
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

          {/* Interactive Student Dashboard Sneak-Peek with Target Streak Calendar */}
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
