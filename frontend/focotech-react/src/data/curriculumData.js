// Complete 3-Course, 3-Subject Curriculum Data Hierarchy
// Matching the Warm Radiant EdTech Theme

export const coursesCurriculum = [
  {
    id: 'course-ui-ux',
    courseName: 'Advance UI/UX Design',
    category: 'Product Design Track',
    badge: 'Trending',
    icon: 'architecture',
    subjects: [
      {
        id: 'subject-design-systems',
        subjectName: 'Design Systems & Component Architecture',
        shortDesc: 'Master design tokens, multi-variant atoms, molecules, and layout grids.',
        progress: '18/32 Topics Done',
        chapters: [
          {
            id: 'ch-tokens',
            title: 'Design Tokens & Semantic Variables',
            subSections: [
              {
                id: 'sub-tokens-fund',
                title: 'Fundamentals of Tokens',
                lessons: [
                  'Color Semantics & Primitive Palettes',
                  'Spacing & Layout Scales (4pt/8pt grids)',
                  'Typography Tokens & Line-height Ratios',
                  'Dark Mode & Elevation Variables',
                  'Component-Level Token Mapping'
                ]
              },
              {
                id: 'sub-tokens-adv',
                title: 'Multi-Brand & Multi-Theme Sets',
                lessons: [
                  'Theme Switching Variables in Figma',
                  'Alias & Reference Variables',
                  'Exporting Tokens to JSON & Style Dictionary'
                ]
              }
            ]
          },
          {
            id: 'ch-components',
            title: 'Atomic Components & Variants',
            subSections: [
              {
                id: 'sub-comp-atoms',
                title: 'Atoms & Molecule Building',
                lessons: [
                  'Button States & Icon Slot Combinations',
                  'Input Fields with Validation States',
                  'Badge, Tag & Chip Component Variants',
                  'Compound Modal & Sheet Architecture'
                ]
              }
            ]
          },
          {
            id: 'ch-auto-layout',
            title: 'Advanced Auto-Layout & Constraints',
            subSections: [
              {
                id: 'sub-layout-resp',
                title: 'Fluid Responsive Containers',
                lessons: [
                  'Min/Max Width and Height Constraints',
                  'Wrap and Absolute Positioning in Auto-Layout',
                  'Building Fully Responsive Tables'
                ]
              }
            ]
          },
          {
            id: 'ch-documentation',
            title: 'Design System Governance & Zeroheight',
            subSections: [
              {
                id: 'sub-gov-review',
                title: 'Documentation & Versioning',
                lessons: [
                  'Component Lifecycle & Deprecation Flags',
                  'Publishing Library Updates Safely',
                  'Syncing Figma Components with Storybook'
                ]
              }
            ]
          }
        ]
      },
      {
        id: 'subject-prototyping',
        subjectName: 'Figma Advanced Prototyping & Micro-Interactions',
        shortDesc: 'Physics-based smart animates, scroll drives, and variables-based state machines.',
        progress: '12/28 Topics Done',
        chapters: [
          {
            id: 'ch-smart-animate',
            title: 'Smart Animate & Transition Curves',
            subSections: [
              {
                id: 'sub-smart-fund',
                title: 'Transition Mechanics',
                lessons: [
                  'Matching Layer Names for Smooth Morphing',
                  'Custom Spring Physics & Easing Curves',
                  'Interactive Micro-Toggle Switches',
                  'Progressive Circular Loaders Animation'
                ]
              }
            ]
          },
          {
            id: 'ch-var-prototypes',
            title: 'Variable Logic & Interactive State Machines',
            subSections: [
              {
                id: 'sub-var-state',
                title: 'Conditionals & Expressions',
                lessons: [
                  'Building a Working Shopping Cart Counter',
                  'Conditional Form Field Validations',
                  'Multi-Step Wizard with Memory State'
                ]
              }
            ]
          },
          {
            id: 'ch-advanced-scroll',
            title: 'Nested Frames & Sticky Navigation',
            subSections: [
              {
                id: 'sub-scroll-header',
                title: 'Scroll-Driven UI',
                lessons: [
                  'Sticky Shrinking Header Bar',
                  'Horizontal Carousel with Snap Points',
                  'Parallax Card Progression'
                ]
              }
            ]
          }
        ]
      },
      {
        id: 'subject-research',
        subjectName: 'User Research & Usability Testing',
        shortDesc: 'Qualitative interviews, quantitative analytics, card sorting, and tree testing.',
        progress: '8/22 Topics Done',
        chapters: [
          {
            id: 'ch-qualitative',
            title: 'User Interview Discovery & Personas',
            subSections: [
              {
                id: 'sub-user-disc',
                title: 'Generative Research',
                lessons: [
                  'Formulating Non-Biased Interview Scripts',
                  'Synthesizing Affinity Diagrams',
                  'Data-Driven Persona Mapping'
                ]
              }
            ]
          },
          {
            id: 'ch-usability-lab',
            title: 'Moderated & Unmoderated Usability Tests',
            subSections: [
              {
                id: 'sub-maze-test',
                title: 'Usability Benchmarking',
                lessons: [
                  'Setting Up Task Success Criteria on Maze',
                  'SUS (System Usability Scale) Calculation',
                  'Heatmaps & Clickpath Drop-off Analysis'
                ]
              }
            ]
          },
          {
            id: 'ch-info-arch',
            title: 'Information Architecture & Card Sorting',
            subSections: [
              {
                id: 'sub-ia-tree',
                title: 'Navigation Hierarchy',
                lessons: [
                  'Open vs Closed Card Sorting Studies',
                  'Treejack Testing for Sitemaps',
                  'Simplifying Complex B2B Menus'
                ]
              }
            ]
          }
        ]
      }
    ]
  },
  {
    id: 'course-web-dev',
    courseName: 'Full-Stack Web Development',
    category: 'Engineering Track',
    badge: 'Popular',
    icon: 'code',
    subjects: [
      {
        id: 'subject-frontend-react',
        subjectName: 'Frontend Architecture with React & Tailwind CSS',
        shortDesc: 'Component lifecycles, custom hooks, performant state, and Tailwind design systems.',
        progress: '24/36 Topics Done',
        chapters: [
          {
            id: 'ch-react-foundations',
            title: 'React 19 & Component Architecture',
            subSections: [
              {
                id: 'sub-react-hooks',
                title: 'Hooks & Modern State',
                lessons: [
                  'useState & useReducer State Machines',
                  'useEffect Cleanup & Event Listeners',
                  'useMemo & useCallback Performance Tuning',
                  'Custom Hooks for Data Fetching & LocalStorage',
                  'Context API vs Zustand for Global Stores'
                ]
              }
            ]
          },
          {
            id: 'ch-tailwind-tokens',
            title: 'Tailwind CSS Modern Styling',
            subSections: [
              {
                id: 'sub-tw-system',
                title: 'Design System Integration',
                lessons: [
                  'Custom Theme Extensions in tailwind.config',
                  'Glassmorphism & Backdrop Blur Filters',
                  'Responsive Flexbox & Grid Patterns',
                  'Custom Keyframe Animations & Easing'
                ]
              }
            ]
          },
          {
            id: 'ch-router-spa',
            title: 'Routing, Modals & SPA Navigation',
            subSections: [
              {
                id: 'sub-spa-history',
                title: 'Client-Side Navigation',
                lessons: [
                  'Hash & History API Route Synchronization',
                  'Animated Modal Dialogs & Trapped Focus',
                  'Dynamic Query Parameter Filters'
                ]
              }
            ]
          }
        ]
      },
      {
        id: 'subject-backend-apis',
        subjectName: 'Backend API Design with Node.js & Databases',
        shortDesc: 'RESTful endpoints, Postgres & Prisma ORM, JWT security, and Redis caching.',
        progress: '14/30 Topics Done',
        chapters: [
          {
            id: 'ch-node-server',
            title: 'Express & RESTful Endpoint Architecture',
            subSections: [
              {
                id: 'sub-api-routes',
                title: 'Request Pipeline & Middleware',
                lessons: [
                  'HTTP Methods, Status Codes & Headers',
                  'Custom Auth & Error Handling Middleware',
                  'Request Body Validation with Zod',
                  'Rate Limiting & CORS Configuration'
                ]
              }
            ]
          },
          {
            id: 'ch-sql-prisma',
            title: 'PostgreSQL & Relational Data Modeling',
            subSections: [
              {
                id: 'sub-db-schemas',
                title: 'Database Operations with Prisma',
                lessons: [
                  'One-to-Many & Many-to-Many Schemas',
                  'Migrations & Database Seeding Scripts',
                  'Indexed Queries for Millisecond Latency'
                ]
              }
            ]
          },
          {
            id: 'ch-auth-jwt',
            title: 'Authentication & Session Security',
            subSections: [
              {
                id: 'sub-jwt-tokens',
                title: 'Secure Access Controls',
                lessons: [
                  'Bcrypt Password Hashing & Salt Rounds',
                  'Access vs Refresh Token Rotation',
                  'Role-Based Access Control (RBAC)'
                ]
              }
            ]
          }
        ]
      },
      {
        id: 'subject-devops-cloud',
        subjectName: 'Full-Stack Deployment & Cloud DevOps',
        shortDesc: 'Docker containers, GitHub Actions CI/CD, AWS S3 buckets, and Vercel edge.',
        progress: '6/20 Topics Done',
        chapters: [
          {
            id: 'ch-docker-env',
            title: 'Containerization with Docker',
            subSections: [
              {
                id: 'sub-docker-compose',
                title: 'Container Orchestration',
                lessons: [
                  'Writing Optimized Multi-Stage Dockerfiles',
                  'Docker Compose for App + PostgreSQL + Redis',
                  'Environment Variables & Secrets Management'
                ]
              }
            ]
          },
          {
            id: 'ch-cicd-pipeline',
            title: 'GitHub Actions Continuous Integration',
            subSections: [
              {
                id: 'sub-actions-workflow',
                title: 'Automated Test & Build',
                lessons: [
                  'Running Linter & Vitest on Pull Requests',
                  'Automated Build & Preview Deployments',
                  'Semantic Versioning & Changelog Automation'
                ]
              }
            ]
          },
          {
            id: 'ch-cloud-hosting',
            title: 'Production Cloud Hosting & Monitoring',
            subSections: [
              {
                id: 'sub-prod-deploy',
                title: 'Vercel, Render & AWS S3',
                lessons: [
                  'Static Asset CDN Distribution',
                  'Server Healthchecks & Uptime Monitoring',
                  'SSL Certificates & Custom Domain DNS'
                ]
              }
            ]
          }
        ]
      }
    ]
  },
  {
    id: 'course-mobile-app',
    courseName: 'Mobile App & Cross-Platform Engineering',
    category: 'Mobile Track',
    badge: 'High Demand',
    icon: 'devices',
    subjects: [
      {
        id: 'subject-react-native',
        subjectName: 'React Native & Mobile UI Fundamentals',
        shortDesc: 'Expo development, touch gestures, navigation stacks, and adaptive layouts.',
        progress: '16/26 Topics Done',
        chapters: [
          {
            id: 'ch-expo-setup',
            title: 'Expo & Core Native Components',
            subSections: [
              {
                id: 'sub-mobile-basics',
                title: 'Primitive Mobile Views',
                lessons: [
                  'View, Text, Image & SafeAreaView Handling',
                  'Flexbox Alignment on iOS vs Android',
                  'FlatList & SectionList Infinite Scrolling',
                  'Platform-Specific Overrides (Platform.OS)'
                ]
              }
            ]
          },
          {
            id: 'ch-mobile-nav',
            title: 'React Navigation (Tabs & Stacks)',
            subSections: [
              {
                id: 'sub-nav-stack',
                title: 'Navigation Hierarchies',
                lessons: [
                  'Native Stack Navigator & Header Styling',
                  'Bottom Tab Navigator with Custom Floating Icons',
                  'Deep Linking & Universal URL Handling'
                ]
              }
            ]
          },
          {
            id: 'ch-reanimated',
            title: 'Fluid Animations with React Native Reanimated',
            subSections: [
              {
                id: 'sub-gesture-drag',
                title: 'Gesture Handler & Physics',
                lessons: [
                  'Swipe-to-Delete Gesture Lists',
                  'Shared Element Transitions between Screens',
                  'Bottom Sheet Drag & Snap Physics'
                ]
              }
            ]
          }
        ]
      },
      {
        id: 'subject-device-apis',
        subjectName: 'Device Hardware APIs & Offline Storage',
        shortDesc: 'Camera, biometric FaceID, push notifications, and SQLite local offline sync.',
        progress: '10/24 Topics Done',
        chapters: [
          {
            id: 'ch-hardware-sensors',
            title: 'Camera, Location & Biometrics',
            subSections: [
              {
                id: 'sub-camera-bio',
                title: 'Native Device Hardware',
                lessons: [
                  'Requesting iOS & Android Runtime Permissions',
                  'Capturing & Compressing Camera Photos',
                  'Local Authentication (FaceID & Fingerprint)',
                  'GPS Geolocation Tracking & Map Markers'
                ]
              }
            ]
          },
          {
            id: 'ch-offline-sync',
            title: 'Offline-First SQLite & MMKV Storage',
            subSections: [
              {
                id: 'sub-local-cache',
                title: 'Local Database & Cache',
                lessons: [
                  'Fast Key-Value Storage with MMKV',
                  'Local SQLite Database CRUD Queries',
                  'Background Sync Engine for Offline Queue'
                ]
              }
            ]
          },
          {
            id: 'ch-push-notifications',
            title: 'Expo Push Notifications & Badges',
            subSections: [
              {
                id: 'sub-push-tokens',
                title: 'Cloud Messaging',
                lessons: [
                  'Registering Push Device Tokens',
                  'Handling Foreground vs Background Notifications',
                  'Scheduling Local Reminders'
                ]
              }
            ]
          }
        ]
      },
      {
        id: 'subject-app-publishing',
        subjectName: 'App Store Publishing & Production Release',
        shortDesc: 'EAS build pipelines, Apple Developer & Google Play Store submission standards.',
        progress: '4/18 Topics Done',
        chapters: [
          {
            id: 'ch-eas-builds',
            title: 'Expo Application Services (EAS)',
            subSections: [
              {
                id: 'sub-eas-config',
                title: 'Cloud Build Automation',
                lessons: [
                  'Configuring eas.json for Staging & Production',
                  'Managing iOS Provisioning Profiles & Certs',
                  'Generating Android AAB App Bundles'
                ]
              }
            ]
          },
          {
            id: 'ch-store-submission',
            title: 'App Store & Google Play Submission',
            subSections: [
              {
                id: 'sub-store-metadata',
                title: 'Review Guidelines & Screenshots',
                lessons: [
                  'App Store Connect Metadata & Privacy Disclosures',
                  'Google Play Data Safety Form Completion',
                  'Handling App Review Rejections Gracefully'
                ]
              }
            ]
          },
          {
            id: 'ch-monitoring-ota',
            title: 'Over-The-Air (OTA) Updates & Sentry',
            subSections: [
              {
                id: 'sub-ota-sentry',
                title: 'Live App Maintenance',
                lessons: [
                  'Pushing Instant Bug Fixes with EAS Update',
                  'Tracking Native Crash Logs with Sentry',
                  'In-App Review Dialogs & Retention Metrics'
                ]
              }
            ]
          }
        ]
      }
    ]
  }
];
