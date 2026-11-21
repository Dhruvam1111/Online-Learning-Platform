import React, { useState, useEffect } from 'react';
import { aiLectureSynthesisAPI } from '../services/api';

export default function AiLectureSynthesisStudio({ lessonId, lessonTitle = 'Current Lesson', onClose }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [successMsg, setSuccessMsg] = useState(null);

  // Upload state
  const [selectedFile, setSelectedFile] = useState(null);
  const [documents, setDocuments] = useState([]);
  const [selectedDocId, setSelectedDocId] = useState(null);
  const [customTranscript, setCustomTranscript] = useState('');

  // Generation options
  const [preferredModel, setPreferredModel] = useState('SOCRATES_OS_PYTHON');
  const [temperature, setTemperature] = useState(0.2);
  const [questionCount, setQuestionCount] = useState(5);
  const [includeFlashcards, setIncludeFlashcards] = useState(true);
  const [customFocus, setCustomFocus] = useState('');

  // Synthesis state
  const [synthesis, setSynthesis] = useState(null);
  const [activeTab, setActiveTab] = useState('guide'); // 'guide', 'glossary', 'nuances', 'quiz', 'flashcards'
  const [isEditingGuide, setIsEditingGuide] = useState(false);
  const [editedGuide, setEditedGuide] = useState('');
  const [editedSummary, setEditedSummary] = useState('');

  // Publish modal state
  const [showPublishModal, setShowPublishModal] = useState(false);
  const [exportToBank, setExportToBank] = useState(true);
  const [targetTopic, setTargetTopic] = useState('');

  useEffect(() => {
    if (lessonId) {
      loadDocuments();
      loadLatestSynthesis();
    }
  }, [lessonId]);

  const loadDocuments = async () => {
    try {
      const res = await aiLectureSynthesisAPI.getNotesDocuments(lessonId);
      setDocuments(res.data || []);
      if (res.data && res.data.length > 0) {
        setSelectedDocId(res.data[0].id);
      }
    } catch (err) {
      console.error('Failed to load notes documents', err);
    }
  };

  const loadLatestSynthesis = async () => {
    try {
      const res = await aiLectureSynthesisAPI.getLatestSynthesis(lessonId);
      if (res.data) {
        setSynthesis(res.data);
        setEditedGuide(res.data.fullStudyGuideMarkdown || '');
        setEditedSummary(res.data.executiveSummary || '');
      }
    } catch (err) {
      // 404 is normal if no synthesis has been generated yet
      setSynthesis(null);
    }
  };

  const handleUploadFile = async (e) => {
    e.preventDefault();
    if (!selectedFile) return;

    setLoading(true);
    setError(null);
    try {
      const formData = new FormData();
      formData.append('file', selectedFile);
      const res = await aiLectureSynthesisAPI.uploadNotesDocument(lessonId, formData);
      setSuccessMsg('Notes document ingested and text extracted successfully.');
      setSelectedFile(null);
      await loadDocuments();
      if (res.data) setSelectedDocId(res.data.id);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to upload notes document.');
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateSynthesis = async () => {
    setLoading(true);
    setError(null);
    setSuccessMsg(null);
    try {
      const payload = {
        notesDocumentId: selectedDocId,
        transcriptText: customTranscript,
        preferredModel,
        temperature,
        questionCount,
        includeFlashcards,
        customFocusInstructions: customFocus,
      };
      const res = await aiLectureSynthesisAPI.triggerSynthesis(lessonId, payload);
      setSynthesis(res.data);
      setEditedGuide(res.data.fullStudyGuideMarkdown || '');
      setEditedSummary(res.data.executiveSummary || '');
      setSuccessMsg('AI Lecture Synthesis generated successfully in DRAFT mode.');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to generate AI synthesis.');
    } finally {
      setLoading(false);
    }
  };

  const handleSaveDraft = async () => {
    if (!synthesis) return;
    setLoading(true);
    setError(null);
    try {
      const payload = {
        executiveSummary: editedSummary,
        fullStudyGuideMarkdown: editedGuide,
        glossary: synthesis.glossary,
        spokenNuances: synthesis.spokenNuances,
        quizQuestions: synthesis.quizQuestions,
        flashcards: synthesis.flashcards,
      };
      const res = await aiLectureSynthesisAPI.updateDraft(synthesis.id, payload);
      setSynthesis(res.data);
      setIsEditingGuide(false);
      setSuccessMsg('Draft updates saved.');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save draft updates.');
    } finally {
      setLoading(false);
    }
  };

  const handleConfirmPublish = async () => {
    if (!synthesis) return;
    setLoading(true);
    setError(null);
    try {
      const payload = {
        publishStudyGuide: true,
        exportQuestionsToQuestionBank: exportToBank,
        targetTopicTag: targetTopic || lessonTitle,
        publishFlashcards: true,
      };
      const res = await aiLectureSynthesisAPI.publishSynthesis(synthesis.id, payload);
      setSynthesis(res.data);
      setShowPublishModal(false);
      setSuccessMsg('Lecture synthesis successfully approved and published to enrolled students.');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to publish synthesis.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-white rounded-xl shadow-lg border border-slate-200 overflow-hidden flex flex-col my-6">
      {/* Header */}
      <div className="bg-slate-900 text-white px-6 py-4 flex flex-wrap items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <span className="text-xs uppercase tracking-wider font-semibold px-2.5 py-0.5 rounded bg-indigo-500/20 text-indigo-300 border border-indigo-400/30">
              AI Synthesis Studio
            </span>
            {synthesis && (
              <span
                className={`text-xs px-2.5 py-0.5 rounded font-medium ${
                  synthesis.reviewStatus === 'PUBLISHED'
                    ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-400/30'
                    : 'bg-amber-500/20 text-amber-300 border border-amber-400/30'
                }`}
              >
                {synthesis.reviewStatus}
              </span>
            )}
          </div>
          <h2 className="text-xl font-bold mt-1 text-slate-100">{lessonTitle}</h2>
        </div>
        <div className="flex items-center gap-2">
          {onClose && (
            <button
              onClick={onClose}
              className="px-3 py-1.5 text-xs text-slate-300 hover:text-white border border-slate-700 hover:border-slate-500 rounded"
            >
              Close Studio
            </button>
          )}
        </div>
      </div>

      {/* Alert Notices */}
      {error && (
        <div className="mx-6 mt-4 p-3 bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg">
          {error}
        </div>
      )}
      {successMsg && (
        <div className="mx-6 mt-4 p-3 bg-emerald-50 border border-emerald-200 text-emerald-700 text-sm rounded-lg">
          {successMsg}
        </div>
      )}

      {/* Main Studio Body */}
      <div className="p-6 grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left Column: Document Ingestion & Generation Controls */}
        <div className="lg:col-span-4 flex flex-col gap-5 border-r border-slate-200 pr-0 lg:pr-6">
          <div className="bg-slate-50 p-4 rounded-lg border border-slate-200">
            <h3 className="text-sm font-semibold text-slate-800 uppercase tracking-wide mb-3">
              1. Ingest Lecture Notes
            </h3>
            <form onSubmit={handleUploadFile} className="space-y-3">
              <div>
                <label className="block text-xs font-medium text-slate-600 mb-1">
                  Upload PDF, Markdown, or Slides
                </label>
                <input
                  type="file"
                  accept=".pdf,.md,.markdown,.txt,.tex"
                  onChange={(e) => setSelectedFile(e.target.files[0])}
                  className="block w-full text-xs text-slate-500 file:mr-2 file:py-1.5 file:px-3 file:rounded file:border-0 file:text-xs file:font-semibold file:bg-indigo-50 file:text-indigo-700 hover:file:bg-indigo-100"
                />
              </div>
              <button
                type="submit"
                disabled={!selectedFile || loading}
                className="w-full py-2 px-3 text-xs font-semibold bg-indigo-600 hover:bg-indigo-700 text-white rounded transition disabled:opacity-50"
              >
                {loading ? 'Processing...' : 'Upload & Extract Text'}
              </button>
            </form>

            {documents.length > 0 && (
              <div className="mt-4 pt-3 border-t border-slate-200">
                <label className="block text-xs font-medium text-slate-600 mb-1">
                  Active Document Source
                </label>
                <select
                  value={selectedDocId || ''}
                  onChange={(e) => setSelectedDocId(Number(e.target.value))}
                  className="w-full text-xs p-2 border border-slate-300 rounded bg-white text-slate-800"
                >
                  {documents.map((doc) => (
                    <option key={doc.id} value={doc.id}>
                      {doc.fileName} ({doc.textExtractionStatus})
                    </option>
                  ))}
                </select>
              </div>
            )}
          </div>

          {/* Transcript Input */}
          <div className="bg-slate-50 p-4 rounded-lg border border-slate-200">
            <h3 className="text-sm font-semibold text-slate-800 uppercase tracking-wide mb-2">
              2. Spoken Transcript
            </h3>
            <p className="text-xs text-slate-500 mb-2">
              Provide or override WebVTT / time-indexed cues [HH:MM:SS]:
            </p>
            <textarea
              rows={3}
              value={customTranscript}
              onChange={(e) => setCustomTranscript(e.target.value)}
              placeholder="00:04:15 - Let us examine randomized leader election timeouts..."
              className="w-full text-xs p-2.5 border border-slate-300 rounded bg-white text-slate-800 font-mono"
            />
          </div>

          {/* Synthesis Parameters */}
          <div className="bg-slate-50 p-4 rounded-lg border border-slate-200 space-y-3">
            <h3 className="text-sm font-semibold text-slate-800 uppercase tracking-wide mb-1">
              3. Socrates-OS Parameters
            </h3>
            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Model Engine</label>
              <select
                value={preferredModel}
                onChange={(e) => setPreferredModel(e.target.value)}
                className="w-full text-xs p-2 border border-slate-300 rounded bg-white text-slate-800"
              >
                <option value="SOCRATES_OS_PYTHON">Socrates-OS (Local Python Engine)</option>
                <option value="GOOGLE_GEMINI">Google Gemini 1.5 Pro</option>
                <option value="OPENAI_GPT">OpenAI GPT-4o</option>
                <option value="ANTHROPIC_CLAUDE">Anthropic Claude 3.5 Sonnet</option>
              </select>
            </div>

            <div className="flex items-center justify-between gap-4">
              <div className="flex-1">
                <label className="block text-xs font-medium text-slate-600 mb-1">Questions</label>
                <select
                  value={questionCount}
                  onChange={(e) => setQuestionCount(Number(e.target.value))}
                  className="w-full text-xs p-2 border border-slate-300 rounded bg-white text-slate-800"
                >
                  <option value={3}>3 Questions</option>
                  <option value={5}>5 Questions</option>
                  <option value={8}>8 Questions</option>
                </select>
              </div>
              <div className="flex items-center pt-5">
                <label className="inline-flex items-center text-xs font-medium text-slate-700">
                  <input
                    type="checkbox"
                    checked={includeFlashcards}
                    onChange={(e) => setIncludeFlashcards(e.target.checked)}
                    className="mr-1.5 rounded border-slate-300 text-indigo-600"
                  />
                  Flashcards
                </label>
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-600 mb-1">Custom Pedagogical Focus</label>
              <input
                type="text"
                value={customFocus}
                onChange={(e) => setCustomFocus(e.target.value)}
                placeholder="e.g. Emphasize network partition invariants"
                className="w-full text-xs p-2 border border-slate-300 rounded bg-white text-slate-800"
              />
            </div>

            <button
              type="button"
              onClick={handleGenerateSynthesis}
              disabled={loading}
              className="w-full mt-2 py-2.5 px-4 text-xs font-bold uppercase tracking-wider bg-slate-900 hover:bg-slate-800 text-white rounded-lg transition shadow disabled:opacity-50"
            >
              {loading ? 'Synthesizing Knowledge Pyramid...' : 'Generate AI Lecture Synthesis'}
            </button>
          </div>
        </div>

        {/* Right Column: Interactive Review Console */}
        <div className="lg:col-span-8 flex flex-col">
          {synthesis ? (
            <div className="flex flex-col h-full">
              {/* Tab Navigation */}
              <div className="flex flex-wrap items-center justify-between border-b border-slate-200 pb-2 mb-4 gap-2">
                <div className="flex space-x-1">
                  {[
                    { id: 'guide', label: 'Study Guide' },
                    { id: 'glossary', label: `Glossary (${synthesis.glossary?.length || 0})` },
                    { id: 'nuances', label: `Nuances (${synthesis.spokenNuances?.length || 0})` },
                    { id: 'quiz', label: `Quiz Bank (${synthesis.quizQuestions?.length || 0})` },
                    { id: 'flashcards', label: `Flashcards (${synthesis.flashcards?.length || 0})` },
                  ].map((tab) => (
                    <button
                      key={tab.id}
                      onClick={() => setActiveTab(tab.id)}
                      className={`px-3 py-1.5 text-xs font-semibold rounded-md transition ${
                        activeTab === tab.id
                          ? 'bg-indigo-600 text-white'
                          : 'text-slate-600 hover:bg-slate-100'
                      }`}
                    >
                      {tab.label}
                    </button>
                  ))}
                </div>

                {/* Review Gate Actions */}
                <div className="flex items-center gap-2">
                  <button
                    onClick={handleSaveDraft}
                    disabled={loading}
                    className="px-3 py-1.5 text-xs font-semibold border border-slate-300 hover:border-slate-400 text-slate-700 rounded transition"
                  >
                    Save Draft
                  </button>
                  <button
                    onClick={() => setShowPublishModal(true)}
                    disabled={loading}
                    className="px-4 py-1.5 text-xs font-semibold bg-emerald-600 hover:bg-emerald-700 text-white rounded transition shadow-sm"
                  >
                    Approve & Publish
                  </button>
                </div>
              </div>

              {/* Tab 1: Study Guide */}
              {activeTab === 'guide' && (
                <div className="space-y-4 flex-1">
                  <div className="flex justify-between items-center">
                    <span className="text-xs text-slate-500">
                      Tokens: {synthesis.promptTokensUsed || 0} in / {synthesis.completionTokensUsed || 0} out
                    </span>
                    <button
                      onClick={() => setIsEditingGuide(!isEditingGuide)}
                      className="text-xs text-indigo-600 hover:underline font-medium"
                    >
                      {isEditingGuide ? 'Preview Rendered Markdown' : 'Edit Markdown Directly'}
                    </button>
                  </div>

                  {isEditingGuide ? (
                    <div className="space-y-3">
                      <div>
                        <label className="block text-xs font-semibold text-slate-700 mb-1">
                          Executive Summary
                        </label>
                        <textarea
                          rows={3}
                          value={editedSummary}
                          onChange={(e) => setEditedSummary(e.target.value)}
                          className="w-full text-xs p-3 border border-slate-300 rounded font-mono text-slate-800"
                        />
                      </div>
                      <div>
                        <label className="block text-xs font-semibold text-slate-700 mb-1">
                          Full Study Guide Markdown
                        </label>
                        <textarea
                          rows={14}
                          value={editedGuide}
                          onChange={(e) => setEditedGuide(e.target.value)}
                          className="w-full text-xs p-3 border border-slate-300 rounded font-mono text-slate-800"
                        />
                      </div>
                    </div>
                  ) : (
                    <div className="p-5 bg-slate-50 border border-slate-200 rounded-lg max-h-[550px] overflow-y-auto">
                      <div className="p-3 mb-4 bg-indigo-50/70 border border-indigo-200 rounded text-xs text-indigo-900 font-medium">
                        <strong>Pedagogical Thesis:</strong> {synthesis.executiveSummary}
                      </div>
                      <div className="prose prose-sm max-w-none text-slate-800 whitespace-pre-wrap font-sans">
                        {synthesis.fullStudyGuideMarkdown}
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* Tab 2: Glossary */}
              {activeTab === 'glossary' && (
                <div className="overflow-x-auto border border-slate-200 rounded-lg">
                  <table className="min-w-full divide-y divide-slate-200 text-xs">
                    <thead className="bg-slate-100 text-slate-700 font-semibold">
                      <tr>
                        <th className="px-4 py-2 text-left">Term</th>
                        <th className="px-4 py-2 text-left">Formal Definition</th>
                        <th className="px-4 py-2 text-left">Intuition</th>
                        <th className="px-4 py-2 text-center">Timestamp</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-200 bg-white">
                      {synthesis.glossary?.map((item, idx) => (
                        <tr key={idx} className="hover:bg-slate-50">
                          <td className="px-4 py-2.5 font-bold text-slate-900">{item.term}</td>
                          <td className="px-4 py-2.5 text-slate-700">{item.formalDefinition}</td>
                          <td className="px-4 py-2.5 text-slate-600 italic">{item.intuition}</td>
                          <td className="px-4 py-2.5 text-center font-mono text-indigo-600">
                            [{item.timestamp}]
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}

              {/* Tab 3: Spoken Nuances */}
              {activeTab === 'nuances' && (
                <div className="space-y-3">
                  {synthesis.spokenNuances?.map((nuance, idx) => (
                    <div key={idx} className="p-4 bg-slate-50 border border-slate-200 rounded-lg">
                      <div className="flex justify-between items-center mb-1">
                        <span className="font-bold text-sm text-slate-900">{nuance.topic}</span>
                        <span className="text-xs font-mono text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded">
                          [{nuance.timestamp}]
                        </span>
                      </div>
                      <div className="text-xs text-slate-700 mb-2">
                        <strong className="text-emerald-700">Verbal Spoken Insight:</strong> {nuance.verbalInsight}
                      </div>
                      <div className="text-xs text-slate-500">
                        <strong className="text-slate-600">Note Alignment Context:</strong> {nuance.noteContext}
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {/* Tab 4: Quiz Questions */}
              {activeTab === 'quiz' && (
                <div className="space-y-4 max-h-[550px] overflow-y-auto pr-1">
                  {synthesis.quizQuestions?.map((q, idx) => (
                    <div key={idx} className="p-4 border border-slate-200 rounded-lg bg-white shadow-sm">
                      <div className="flex items-center justify-between mb-2">
                        <span className="text-xs font-bold uppercase tracking-wider text-indigo-600">
                          Question {idx + 1} &bull; {q.questionType}
                        </span>
                        <span className="text-xs px-2 py-0.5 rounded font-medium bg-purple-50 text-purple-700 border border-purple-200">
                          Bloom: {q.bloomLevel}
                        </span>
                      </div>
                      <p className="text-sm font-semibold text-slate-900 mb-3">{q.questionText}</p>
                      <div className="space-y-1.5 mb-3">
                        {q.options?.map((opt, optIdx) => {
                          const isCorrect = q.correctAnswers?.includes(opt);
                          return (
                            <div
                              key={optIdx}
                              className={`p-2 rounded text-xs border ${
                                isCorrect
                                  ? 'bg-emerald-50 border-emerald-300 text-emerald-900 font-medium'
                                  : 'bg-slate-50 border-slate-200 text-slate-700'
                              }`}
                            >
                              {opt} {isCorrect && '(Correct)'}
                            </div>
                          );
                        })}
                      </div>
                      <div className="p-2.5 bg-slate-50 rounded text-xs text-slate-600 border border-slate-200">
                        <strong>Explanation:</strong> {q.explanation}
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {/* Tab 5: Flashcards */}
              {activeTab === 'flashcards' && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {synthesis.flashcards?.map((card, idx) => (
                    <div key={idx} className="p-4 border border-slate-200 rounded-lg bg-slate-50 flex flex-col justify-between">
                      <div>
                        <span className="text-xs font-bold text-slate-400 uppercase tracking-wide">
                          Card {idx + 1} &bull; {card.topic}
                        </span>
                        <p className="text-sm font-semibold text-slate-900 mt-1 mb-3">{card.front}</p>
                      </div>
                      <div className="p-3 bg-white border border-slate-200 rounded text-xs text-slate-700">
                        {card.back}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center p-12 text-center bg-slate-50 border border-dashed border-slate-300 rounded-xl">
              <h3 className="text-base font-semibold text-slate-700 mb-1">
                No Synthesis Generated Yet
              </h3>
              <p className="text-xs text-slate-500 max-w-sm mb-4">
                Upload lecture notes or paste a transcript on the left, then click "Generate AI Lecture Synthesis" to build the 4-tier knowledge suite.
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Publish Modal */}
      {showPublishModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4">
          <div className="bg-white rounded-xl shadow-2xl max-w-md w-full p-6 border border-slate-200">
            <h3 className="text-base font-bold text-slate-900 mb-2">
              Human-in-the-Loop Review Gate (FR-I55)
            </h3>
            <p className="text-xs text-slate-600 mb-4">
              Confirm publication of this synthesis. Once published, enrolled students can view the study guide, glossary, and flashcards.
            </p>
            <div className="space-y-3 mb-6 bg-slate-50 p-4 rounded-lg border border-slate-200">
              <label className="flex items-center text-xs font-medium text-slate-800">
                <input
                  type="checkbox"
                  checked={exportToBank}
                  onChange={(e) => setExportToBank(e.target.checked)}
                  className="mr-2 rounded text-indigo-600"
                />
                Export Diagnostic Questions to Central Question Bank
              </label>
              {exportToBank && (
                <div>
                  <label className="block text-xs font-medium text-slate-600 mb-1">
                    Question Bank Topic Tag
                  </label>
                  <input
                    type="text"
                    value={targetTopic}
                    onChange={(e) => setTargetTopic(e.target.value)}
                    placeholder={lessonTitle}
                    className="w-full text-xs p-2 border border-slate-300 rounded bg-white text-slate-800"
                  />
                </div>
              )}
            </div>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setShowPublishModal(false)}
                className="px-4 py-2 text-xs font-semibold text-slate-600 hover:text-slate-800"
              >
                Cancel
              </button>
              <button
                onClick={handleConfirmPublish}
                disabled={loading}
                className="px-4 py-2 text-xs font-semibold bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg shadow-sm"
              >
                Confirm & Publish
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
