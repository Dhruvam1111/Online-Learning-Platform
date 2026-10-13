export const UNIVERSITIES = [
  { name: 'Stanford University', domain: 'stanford.edu', mascot: 'Cardinal', color: '#8C1515', badge: 'Tier 1 Partner' },
  { name: 'Harvard University', domain: 'harvard.edu', mascot: 'Crimson', color: '#A51C30', badge: 'Tier 1 Partner' },
  { name: 'Massachusetts Institute of Technology (MIT)', domain: 'mit.edu', mascot: 'Engineers', color: '#A31F34', badge: 'Tier 1 Partner' },
  { name: 'UC Berkeley', domain: 'berkeley.edu', mascot: 'Golden Bears', color: '#003262', badge: 'Campus Partner' },
  { name: 'Columbia University', domain: 'columbia.edu', mascot: 'Lions', color: '#75B2DD', badge: 'Campus Partner' },
  { name: 'University of Oxford', domain: 'ox.ac.uk', mascot: 'Dark Blues', color: '#002147', badge: 'Global Partner' },
  { name: 'University of Cambridge', domain: 'cam.ac.uk', mascot: 'Light Blues', color: '#A3C1AD', badge: 'Global Partner' },
  { name: 'New York University (NYU)', domain: 'nyu.edu', mascot: 'Bobcats', color: '#57068C', badge: 'Campus Partner' },
  { name: 'University of Toronto', domain: 'utoronto.ca', mascot: 'Varsity Blues', color: '#002A5C', badge: 'Campus Partner' },
  { name: 'Indian Institute of Technology (IIT)', domain: 'iit.ac.in', mascot: 'Innovators', color: '#D97706', badge: 'National Partner' },
  { name: 'National University of Singapore (NUS)', domain: 'nus.edu.sg', mascot: 'Lions', color: '#EF7C00', badge: 'Global Partner' },
  { name: 'University of California, Los Angeles (UCLA)', domain: 'ucla.edu', mascot: 'Bruins', color: '#2774AE', badge: 'Campus Partner' },
  { name: 'University of Michigan', domain: 'umich.edu', mascot: 'Wolverines', color: '#00274C', badge: 'Campus Partner' },
  { name: 'Other College / University', domain: '', mascot: 'Scholars', color: '#b52603', badge: 'Verified Student' }
];

export const ACADEMIC_YEARS = [
  { id: 'freshman', label: 'Freshman (1st Year)', gradYear: "'28", icon: 'looks_one' },
  { id: 'sophomore', label: 'Sophomore (2nd Year)', gradYear: "'27", icon: 'looks_two' },
  { id: 'junior', label: 'Junior (3rd Year)', gradYear: "'26", icon: 'looks_3' },
  { id: 'senior', label: 'Senior (4th Year)', gradYear: "'25", icon: 'looks_4' },
  { id: 'grad', label: 'Graduate / Masters / PhD', gradYear: "'Adv", icon: 'school' }
];

export const DISCIPLINES = [
  {
    id: 'business',
    name: 'Business & Economics',
    shortName: 'Business',
    icon: 'monitoring',
    emoji: '📊',
    color: 'from-amber-500 to-orange-600',
    borderColor: 'border-amber-400/50',
    bgLight: 'bg-amber-500/10 text-amber-700',
    accentHex: '#d97706',
    tagline: 'Finance, Marketing, Management & Entrepreneurship',
    popularInDiscipline: 'Financial Modeling, Case Study Competitions, Startup Strategy',
    stat: '4,850+ Business majors collaborating'
  },
  {
    id: 'health',
    name: 'Medicine & Health Sciences',
    shortName: 'Pre-Med & Health',
    icon: 'ecg_heart',
    emoji: '🩺',
    color: 'from-emerald-500 to-teal-600',
    borderColor: 'border-emerald-400/50',
    bgLight: 'bg-emerald-500/10 text-emerald-700',
    accentHex: '#059669',
    tagline: 'Pre-Med, Biology, Neuroscience & Public Health',
    popularInDiscipline: 'Clinical Anatomy, MCAT Foundations, Molecular Genetics',
    stat: '3,920+ Pre-Med students on flashcard tracks'
  },
  {
    id: 'law',
    name: 'Law, Policy & Humanities',
    shortName: 'Law & Humanities',
    icon: 'gavel',
    emoji: '⚖️',
    color: 'from-indigo-500 to-purple-600',
    borderColor: 'border-indigo-400/50',
    bgLight: 'bg-indigo-500/10 text-indigo-700',
    accentHex: '#4f46e5',
    tagline: 'Pre-Law, Political Science, Philosophy & History',
    popularInDiscipline: 'Constitutional Moot Court, Legal Writing, Applied Ethics',
    stat: '2,640+ Humanities scholars in debate pods'
  },
  {
    id: 'arts',
    name: 'Arts, Media & Architecture',
    shortName: 'Arts & Media',
    icon: 'palette',
    emoji: '🎨',
    color: 'from-rose-500 to-pink-600',
    borderColor: 'border-rose-400/50',
    bgLight: 'bg-rose-500/10 text-rose-700',
    accentHex: '#e11d48',
    tagline: 'Design, Fine Arts, Film, Journalism & Architecture',
    popularInDiscipline: 'Portfolio Critiques, Studio Urbanism, Documentary Cinema',
    stat: '3,110+ Design & Art students showcasing work'
  },
  {
    id: 'sciences',
    name: 'Natural Sciences & Math',
    shortName: 'Pure Sciences',
    icon: 'science',
    emoji: '🔬',
    color: 'from-cyan-500 to-blue-600',
    borderColor: 'border-cyan-400/50',
    bgLight: 'bg-cyan-500/10 text-cyan-700',
    accentHex: '#0284c7',
    tagline: 'Physics, Chemistry, Pure Math & Statistics',
    popularInDiscipline: 'Quantum Mechanics, Organic Synthesis, Linear Algebra',
    stat: '2,480+ Science students solving problem sets'
  },
  {
    id: 'engineering',
    name: 'Engineering & Computing',
    shortName: 'Engineering',
    icon: 'terminal',
    emoji: '⚙️',
    color: 'from-orange-500 to-red-600',
    borderColor: 'border-orange-400/50',
    bgLight: 'bg-orange-500/10 text-orange-700',
    accentHex: '#ea580c',
    tagline: 'Robotics, Software, Mechanical & Systems',
    popularInDiscipline: 'Embedded Systems, AI Algorithms, Distributed Clouds',
    stat: '5,300+ Engineering students in lab cohorts'
  }
];

export const STUDY_GOALS = [
  { id: 'exams', label: 'Ace Upcoming Midterms & Finals', icon: 'military_tech', emoji: '🎯' },
  { id: 'internship', label: 'Land a Summer Internship / Job', icon: 'work', emoji: '💼' },
  { id: 'peers', label: 'Find Campus Study Pods & Peers', icon: 'groups', emoji: '🤝' },
  { id: 'explore', label: 'Explore Electives Outside My Major', icon: 'explore', emoji: '🧭' }
];

export const DEMO_STUDENTS = [
  {
    id: 'demo-maya',
    name: 'Maya Lin',
    email: 'maya.lin@stanford.edu',
    password: 'Password@2026',
    university: 'Stanford University',
    disciplineId: 'health',
    majorName: 'Pre-Med & Human Biology',
    year: 'sophomore',
    studentId: 'STAN-2027-MED89',
    streakDays: 14,
    avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
    avatarInitials: 'ML',
    goal: 'exams',
    bio: 'Aspiring pediatric surgeon & neuroscience researcher. Balancing lab work with MCAT study pods.'
  },
  {
    id: 'demo-marcus',
    name: 'Marcus Vance',
    email: 'marcus.vance@wharton.upenn.edu',
    password: 'Password@2026',
    university: 'Columbia University',
    disciplineId: 'business',
    majorName: 'Finance & Venture Economics',
    year: 'junior',
    studentId: 'COL-2026-FIN42',
    streakDays: 28,
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80',
    avatarInitials: 'MV',
    goal: 'internship',
    bio: 'Senior investment society president. Passionate about sustainable venture models & market analysis.'
  },
  {
    id: 'demo-elena',
    name: 'Elena Rostova',
    email: 'elena.rostova@ox.ac.uk',
    password: 'Password@2026',
    university: 'University of Oxford',
    disciplineId: 'law',
    majorName: 'International Law & Human Rights',
    year: 'senior',
    studentId: 'OXF-2025-LAW19',
    streakDays: 45,
    avatar: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150&auto=format&fit=crop&q=80',
    avatarInitials: 'ER',
    goal: 'internship',
    bio: 'Moot court champion & legal policy analyst. Researching AI ethics and global intellectual property.'
  },
  {
    id: 'demo-liam',
    name: 'Liam Patel',
    email: 'liam.patel@berkeley.edu',
    password: 'Password@2026',
    university: 'UC Berkeley',
    disciplineId: 'arts',
    majorName: 'Sustainable Architecture & Media Design',
    year: 'freshman',
    studentId: 'CAL-2028-ARC07',
    streakDays: 7,
    avatar: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop&q=80',
    avatarInitials: 'LP',
    goal: 'explore',
    bio: 'Designing bio-integrated urban spaces. Learning computational parametric modeling & 3D visualization.'
  }
];

export const LIVE_CAMPUS_FEED = [
  { name: 'Devon W.', university: 'Harvard', major: 'Economics', action: 'completed Financial Statement Analysis', time: 'Just now' },
  { name: 'Aaliyah K.', university: 'Johns Hopkins', major: 'Pre-Med', action: 'joined Neurobiology Peer Study Pod', time: '1m ago' },
  { name: 'Mateo R.', university: 'Toronto', major: 'Architecture', action: 'shared Urban Design Portfolio draft', time: '3m ago' },
  { name: 'Chloe T.', university: 'Oxford', major: 'Constitutional Law', action: 'scored 98% on Appellate Advocacy Quiz', time: '4m ago' },
  { name: 'Siddharth M.', university: 'IIT Delhi', major: 'Robotics', action: 'unlocked Autonomous Systems Lab badge', time: '6m ago' }
];
