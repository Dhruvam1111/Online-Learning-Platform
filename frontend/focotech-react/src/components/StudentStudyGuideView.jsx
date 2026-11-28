import React, { useState, useEffect } from 'react';
import { aiLectureSynthesisAPI } from '../services/api';

export default function StudentStudyGuideView({ lessonId, lessonTitle = 'Lesson' }) {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [synthesis, setSynthesis] = useState(null);
  const [activeTab, setActiveTab] = useState('guide'); // 'guide', 'glossary', 'flashcards'
  const [flippedCards, setFlippedCards] = useState({});

  useEffect(() => {
    if (lessonId) {
      loadStudyGuide();
    }
  }, [lessonId]);

  const loadStudyGuide = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await aiLectureSynthesisAPI.getStudentStudyGuide(lessonId);
      setSynthesis(res.data);
    } catch (err) {
      if (err.response?.status === 404) {
        setError('No published AI study guide is available for this lesson yet.');
      } else {
        setError('Failed to load study guide.');
      }
    } finally {
      setLoading(false);
    }
  };

  const toggleFlip = (id) => {
    setFlippedCards((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  if (loading) {
    return (
      <div className="p-8 text-center text-xs text-slate-500 bg-slate-50 rounded-xl border border-slate-200 my-4">
        Loading published AI study guide...
      </div>
    );
  }

  if (error || !synthesis) {
    return (
      <div className="p-6 text-center bg-slate-50 rounded-xl border border-slate-200 my-4">
        <p className="text-xs text-slate-500">{error || 'Study guide not available.'}</p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden my-4">
      {/* Header */}
      <div className="bg-slate-900 text-white px-6 py-4 flex flex-wrap items-center justify-between gap-4">
        <div>
          <span className="text-xs uppercase tracking-wider font-semibold px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300 border border-emerald-400/30">
            Verified Study Guide
          </span>
          <h2 className="text-lg font-bold text-slate-100 mt-1">{lessonTitle}</h2>
        </div>
        <div className="flex space-x-1">
          {[
            { id: 'guide', label: 'Study Guide' },
            { id: 'glossary', label: `Glossary (${synthesis.glossary?.length || 0})` },
            { id: 'flashcards', label: `Flashcards (${synthesis.flashcards?.length || 0})` },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`px-3 py-1.5 text-xs font-semibold rounded-md transition ${
                activeTab === tab.id
                  ? 'bg-indigo-600 text-white'
                  : 'text-slate-300 hover:bg-slate-800'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      <div className="p-6">
        {/* Tab 1: Rendered Guide */}
        {activeTab === 'guide' && (
          <div className="space-y-4">
            <div className="p-4 bg-indigo-50/70 border border-indigo-200 rounded-lg text-xs text-indigo-950 font-medium">
              <strong>Core Takeaway:</strong> {synthesis.executiveSummary}
            </div>
            <div className="prose prose-sm max-w-none text-slate-800 whitespace-pre-wrap font-sans p-4 bg-slate-50 border border-slate-200 rounded-lg max-h-[600px] overflow-y-auto">
              {synthesis.fullStudyGuideMarkdown}
            </div>
          </div>
        )}

        {/* Tab 2: Glossary */}
        {activeTab === 'glossary' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {synthesis.glossary?.map((item, idx) => (
              <div key={idx} className="p-4 bg-slate-50 border border-slate-200 rounded-lg flex flex-col justify-between">
                <div>
                  <div className="flex justify-between items-center mb-1">
                    <span className="font-bold text-sm text-slate-900">{item.term}</span>
                    <span className="text-xs font-mono text-indigo-600">[{item.timestamp}]</span>
                  </div>
                  <p className="text-xs text-slate-700 mb-2">{item.formalDefinition}</p>
                </div>
                <div className="pt-2 border-t border-slate-200 text-xs text-slate-500 italic">
                  Intuition: {item.intuition}
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Tab 3: Interactive Active Recall Flashcards */}
        {activeTab === 'flashcards' && (
          <div>
            <p className="text-xs text-slate-500 mb-4">
              Click any card to flip between the question prompt and verified answer.
            </p>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {synthesis.flashcards?.map((card, idx) => {
                const isFlipped = flippedCards[card.id || idx];
                return (
                  <div
                    key={idx}
                    onClick={() => toggleFlip(card.id || idx)}
                    className="cursor-pointer min-h-[160px] p-5 rounded-xl border border-slate-200 bg-slate-50 hover:border-indigo-300 transition shadow-sm flex flex-col justify-between"
                  >
                    <div>
                      <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
                        {card.topic || 'Concept'} &bull; {isFlipped ? 'Answer' : 'Question'}
                      </span>
                      <p className="text-sm font-semibold text-slate-900 mt-2">
                        {isFlipped ? card.back : card.front}
                      </p>
                    </div>
                    <span className="text-xs text-indigo-600 font-medium mt-3 self-end">
                      Click to flip &rarr;
                    </span>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
