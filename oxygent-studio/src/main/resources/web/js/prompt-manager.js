/**
 * Live Prompt Manager - Optimized JavaScript Module
 * High-performance frontend for OxyGent live prompt management
 */

// State management - Optimized
const state = {
    prompts: [],
    filteredPrompts: [],
    currentPrompt: null,
    isLoading: false,
    editOriginalContent: ''
};

// DOM cache - Performance optimization
const dom = {};

// Initialize DOM cache
function initDOMCache() {
    dom.searchInput = document.getElementById('searchInput');
    dom.promptsList = document.getElementById('promptsList');
    dom.editModal = document.getElementById('edit-modal');
    dom.historyModal = document.getElementById('history-modal');
    dom.previewModal = document.getElementById('preview-modal');
    dom.rollbackModal = document.getElementById('rollback-modal');
    dom.optimizeModal = document.getElementById('optimize-modal');
    dom.editForm = document.getElementById('edit-form');
    dom.editKey = document.getElementById('edit_prompt_key');
    dom.editContent = document.getElementById('edit_prompt_content');
    dom.saveButton = document.getElementById('save-prompt-btn');
    dom.versionList = document.getElementById('version-list');
    dom.previewContent = document.getElementById('preview-content');
    dom.previewVersionInfo = document.getElementById('preview-version-info');
    dom.previewDateInfo = document.getElementById('preview-date-info');
    dom.previewStats = document.getElementById('preview-stats');
    dom.previewRollbackBtn = document.getElementById('preview-rollback-btn');
    dom.rollbackMessage = document.getElementById('rollback-message');
    dom.confirmRollbackBtn = document.getElementById('confirm-rollback-btn');
    // Optimize modal elements
    dom.optimizePromptKey = document.getElementById('optimize_prompt_key');
    dom.optimizeAgentType = document.getElementById('optimize_agent_type');
    dom.optimizeStrategy = document.getElementById('optimize_strategy');
    dom.optimizeRequirements = document.getElementById('optimize_requirements');
    dom.optimizeResults = document.getElementById('optimize-results');
    dom.optimizeAnalysis = document.getElementById('optimize-analysis');
    dom.optimizeImprovements = document.getElementById('optimize-improvements');
    dom.optimizeRationale = document.getElementById('optimize-rationale');
    dom.optimizeValidation = document.getElementById('optimize-validation');
    dom.optimizeOriginal = document.getElementById('optimize-original');
    dom.optimizeOptimized = document.getElementById('optimize-optimized');
    dom.previewOptimizeBtn = document.getElementById('preview-optimize-btn');
    dom.applyOptimizeBtn = document.getElementById('apply-optimize-btn');
    dom.agentTypeSource = document.getElementById('agent_type_source');
}

// Optimized debounce with immediate option
const debounce = (func, wait, immediate = false) => {
    let timeout;
    return (...args) => {
        const later = () => {
            timeout = null;
            if (!immediate) func.apply(null, args);
        };
        const callNow = immediate && !timeout;
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
        if (callNow) func.apply(null, args);
    };
};

// Optimized DOM ready
document.addEventListener('DOMContentLoaded', () => {
    console.log('🚀 Live Prompt Manager initialized');
    initDOMCache();
    setupEventListeners();
    loadPrompts();
});

// Event listeners - Optimized with delegation
function setupEventListeners() {
    // Search with optimized debounce
    dom.searchInput?.addEventListener('input', debounce(handleSearchInput, 300));

    // Keyboard shortcuts
    document.addEventListener('keydown', handleKeydown);

    // Modal close handlers - Event delegation
    document.addEventListener('click', handleModalClicks);

    // Form submission
    dom.editForm?.addEventListener('submit', handleFormSubmit);
    dom.editContent?.addEventListener('input', debounce(updateSaveButtonState, 150));
}

function handleSearchInput(e) {
    const query = e.target.value;
    query.length > 0 ? searchPrompts() : resetSearch();
}

function handleKeydown(e) {
    if (e.key === 'Escape') {
        // Close modals in priority order - rollback first, then preview, then history
        // Note: edit-modal cannot be closed with Escape (requires button click)
        if (dom.rollbackModal && dom.rollbackModal.style.display === 'block') {
            hideRollbackModal();
        } else if (dom.previewModal && dom.previewModal.style.display === 'block') {
            hidePreviewModal();
        } else if (dom.historyModal && dom.historyModal.style.display === 'block') {
            hideHistoryModal();
        }
    }
}

function handleModalClicks(e) {
    if (e.target.classList.contains('modal')) {
        // Close the specific modal that was clicked
        // Note: edit-modal cannot be closed by clicking background (requires button click)
        if (e.target.id === 'rollback-modal') {
            hideRollbackModal();
        } else if (e.target.id === 'preview-modal') {
            hidePreviewModal();
        } else if (e.target.id === 'history-modal') {
            hideHistoryModal();
        }
    }
}

function handleFormSubmit(e) {
    e.preventDefault();
    savePrompt();
}

function resetSearch() {
    state.filteredPrompts = [...state.prompts];
    renderPrompts();
}

// API functions - Optimized with error handling
async function loadPrompts() {
    if (state.isLoading) return;

    state.isLoading = true;
    showLoading("Loading prompts...");

    try {
        const response = await fetch('/api/prompts/', {
            headers: { 'Accept': 'application/json' }
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const result = await response.json();

        if (result.success) {
            state.prompts = result.data || [];
            state.filteredPrompts = [...state.prompts];
            renderPrompts();
            console.log(`✅ Loaded ${state.prompts.length} prompts`);
        } else {
            throw new Error(result.message || 'Failed to load prompts');
        }
    } catch (error) {
        console.error('❌ Error loading prompts:', error);
        showError('Failed to load prompts: ' + error.message);
    } finally {
        state.isLoading = false;
    }
}

// Optimized search with better performance
function searchPrompts() {
    const query = dom.searchInput.value.toLowerCase().trim();

    if (!query) {
        resetSearch();
        return;
    }

    // Optimized search algorithm
    state.filteredPrompts = state.prompts.filter(prompt => {
        const searchFields = [
            prompt.prompt_key,
            prompt.prompt_content,
            prompt.agent_type,
            prompt.description
        ].filter(Boolean);

        return searchFields.some(field =>
            field.toLowerCase().includes(query)
        );
    });

    renderPrompts();
    console.log(`🔍 Search "${query}" found ${state.filteredPrompts.length} results`);
}

// Legacy search handler for backward compatibility
function handleSearch(event) {
    if (event.key === 'Enter') {
        searchPrompts();
    }
}

// Optimized rendering with DocumentFragment
function renderPrompts() {
    if (!dom.promptsList) return;

    if (state.filteredPrompts.length === 0) {
        dom.promptsList.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">📝</div>
                <h3>No prompts found</h3>
                <p>Try adjusting your search or check if agents are running.</p>
            </div>
        `;
        return;
    }

    // Use DocumentFragment for better performance
    const fragment = document.createDocumentFragment();

    state.filteredPrompts.forEach(prompt => {
        const card = createPromptCard(prompt);
        fragment.appendChild(card);
    });

    dom.promptsList.innerHTML = '';
    dom.promptsList.appendChild(fragment);
}

// Optimized card creation
function createPromptCard(prompt) {
    const card = document.createElement('div');
    card.className = 'prompt-card';
    card.dataset.promptKey = prompt.prompt_key;

    const truncatedContent = prompt.prompt_content.length > 200
        ? prompt.prompt_content.substring(0, 200) + '...'
        : prompt.prompt_content;

    card.innerHTML = `
        <div class="prompt-card-header">
            <h3 class="prompt-title">${escapeHtml(prompt.prompt_key)}</h3>
            <div class="prompt-meta">
                <span class="badge badge-agent">${escapeHtml(prompt.agent_type || 'General')}</span>
                <span class="badge badge-version">v${prompt.version || 1}</span>
            </div>
        </div>
        <div class="prompt-content">
            ${escapeHtml(truncatedContent)}
        </div>
        <div class="prompt-actions" style="grid-template-columns: 1fr 1fr; gap: 8px;">
            <button class="btn btn-sm btn-primary" onclick="editPrompt('${escapeHtml(prompt.prompt_key)}')">
                ✏️ Edit
                <span class="btn-badge"></span>
            </button>
            <button class="btn btn-sm btn-info" onclick="showOptimizeModal('${escapeHtml(prompt.prompt_key)}')">
                ✨ Optimize
                <span class="btn-badge"></span>
            </button>
            <button class="btn btn-sm btn-primary" onclick="refreshPrompt('${escapeHtml(prompt.prompt_key)}')">
                🔄 Refresh
                <span class="btn-badge"></span>
            </button>
            <button class="btn btn-sm btn-primary" onclick="showVersionHistory('${escapeHtml(prompt.prompt_key)}')">
                📋 History
                <span class="btn-badge"></span>
            </button>
        </div>
    `;

    return card;
}

// Optimized HTML escape
const escapeHtml = (() => {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    const reg = /[&<>"']/g;
    return text => String(text).replace(reg, m => map[m]);
})();

// CRUD Operations - Optimized with better error handling
async function editPrompt(promptKey) {
    try {
        showNotification('Loading prompt...', 'info');

        const response = await fetch(`/api/prompts/${promptKey}`, {
            headers: { 'Accept': 'application/json' }
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const result = await response.json();

        if (result.success) {
            state.currentPrompt = result.data;
            dom.editKey.value = state.currentPrompt.prompt_key;
            dom.editContent.value = state.currentPrompt.prompt_content;
            state.editOriginalContent = state.currentPrompt.prompt_content || '';
            updateSaveButtonState();
            showEditModal();

            // Focus with better UX
            requestAnimationFrame(() => {
                dom.editContent?.focus();
                dom.editContent?.setSelectionRange(0, 0);
            });

            console.log(`📝 Editing prompt: ${promptKey}`);
        } else {
            throw new Error(result.message || 'Failed to load prompt');
        }
    } catch (error) {
        console.error('❌ Error loading prompt:', error);
        showNotification('Failed to load prompt: ' + error.message, 'error');
    }
}

function updateSaveButtonState() {
    if (!dom.saveButton || !dom.editContent) return;

    const currentContent = dom.editContent.value.trim();
    const originalContent = (state.editOriginalContent || '').trim();
    const hasChanges = currentContent !== originalContent;

    dom.saveButton.disabled = !hasChanges;
}

async function savePrompt() {
    const promptKey = dom.editKey.value.trim();
    const promptContent = dom.editContent.value.trim();

    if (!promptKey || !promptContent) {
        showNotification('Please fill in all required fields.', 'error');
        return;
    }

    if (promptContent === (state.editOriginalContent || '').trim()) {
        showNotification('No changes detected. Update the prompt before saving.', 'info');
        return;
    }

    try {
        showNotification('Saving prompt...', 'info');

        const payload = {
            prompt_key: promptKey,
            prompt_content: promptContent,
            agent_type: state.currentPrompt.agent_type,
            version: (state.currentPrompt.version || 1) + 1,
            description: state.currentPrompt.description,
            category: state.currentPrompt.category || 'agent'
        };

        const response = await fetch(`/api/prompts/${promptKey}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const result = await response.json();

        if (result.success) {
            showNotification('Prompt updated successfully!', 'success');
            hideEditModal();
            await loadPrompts(); // Refresh data
            console.log(`✅ Saved prompt: ${promptKey}`);
        } else {
            throw new Error(result.message || 'Save failed');
        }
    } catch (error) {
        console.error('❌ Error saving prompt:', error);
        showNotification('Failed to save prompt: ' + error.message, 'error');
    }
}

async function refreshPrompt(promptKey) {
    try {
        showNotification('Refreshing prompt...', 'info');

        // Simply reload the prompts list to refresh the data
        await loadPrompts();

        showNotification('Prompt refreshed successfully!', 'success');
        console.log(`🔄 Refreshed: ${promptKey}`);
    } catch (error) {
        console.error('❌ Error refreshing prompt:', error);
        showNotification('Failed to refresh prompt: ' + error.message, 'error');
    }
}

async function showVersionHistory(promptKey) {
    try {
        showNotification('Loading version history...', 'info');

        const response = await fetch(`/api/prompts/${promptKey}/history`, {
            headers: { 'Accept': 'application/json' }
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const result = await response.json();

        if (result.success) {
            const versions = result.data || [];
            renderVersionHistory(versions, promptKey);
            showHistoryModal();
            console.log(`📋 Loaded ${versions.length} versions for: ${promptKey}`);
        } else {
            throw new Error(result.message || 'Failed to load version history');
        }
    } catch (error) {
        console.error('❌ Error loading version history:', error);
        showNotification('Failed to load version history: ' + error.message, 'error');
    }
}

// Version History & UI Components - Optimized
function renderVersionHistory(versions, promptKey) {
    if (!dom.versionList) return;

    if (versions.length === 0) {
        dom.versionList.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">📋</div>
                <p>No version history available.</p>
            </div>
        `;
        return;
    }

    // Sort versions efficiently
    versions.sort((a, b) => (b.version || 0) - (a.version || 0));

    const fragment = document.createDocumentFragment();

    versions.forEach(version => {
        const item = document.createElement('div');
        item.className = `version-item${version.is_current ? ' current' : ''}`;

        // Use archived_at for history versions, updated_at for current version
        const timeField = version.archived_at || version.updated_at || version.created_at;
        const date = new Date(timeField).toLocaleString();
        const isCurrent = version.is_current;

        item.innerHTML = `
            <div class="version-info">
                <div class="version-number">
                    Version ${version.version} ${isCurrent ? '(Current)' : ''}
                </div>
                <div class="version-date">📅 ${date}</div>
            </div>
            <div class="version-actions">
                ${!isCurrent ? `<button class="btn btn-sm btn-warning" onclick="rollbackToVersion('${escapeHtml(promptKey)}', ${version.version})">
                    ↩️ Rollback
                    <span class="btn-badge"></span>
                </button>` : ''}
                <button class="btn btn-sm btn-primary" onclick="previewVersion('${escapeHtml(promptKey)}', ${version.version})">
                    👁️ Preview
                    <span class="btn-badge"></span>
                </button>
            </div>
        `;

        fragment.appendChild(item);
    });

    dom.versionList.innerHTML = '';
    dom.versionList.appendChild(fragment);
}

// Enhanced preview function with better modal
let currentPreviewData = null;

async function previewVersion(promptKey, version) {
    try {
        showNotification('Loading version preview...', 'info');

        const response = await fetch(`/api/prompts/${promptKey}/version/${version}`, {
            headers: { 'Accept': 'application/json' }
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const result = await response.json();

        if (result.success) {
            const versionData = result.data;
            currentPreviewData = { promptKey, version, data: versionData };

            // Update preview modal content
            updatePreviewModal(versionData, promptKey, version);
            showPreviewModal();

            console.log(`👁️ Previewed version ${version} of: ${promptKey}`);
        } else {
            throw new Error(result.message || 'Failed to load version');
        }
    } catch (error) {
        console.error('❌ Error loading version:', error);
        showNotification('Failed to load version: ' + error.message, 'error');
    }
}

function updatePreviewModal(versionData, promptKey, version) {
    // Update version info
    dom.previewVersionInfo.textContent = `Version ${version}`;

    // Update date info
    const date = new Date(versionData.created_at || Date.now()).toLocaleString();
    dom.previewDateInfo.innerHTML = `&nbsp;&nbsp;📅 ${date}`;

    // Update stats
    const content = versionData.prompt_content || '';
    const wordCount = content.trim().split(/\s+/).filter(word => word.length > 0).length;
    const charCount = content.length;
    const lineCount = content.split('\n').length;

    dom.previewStats.innerHTML = `
        <span>📝 ${wordCount} words</span>
        <span>🔡 ${charCount} characters</span>
        <span>📄 ${lineCount} lines</span>
    `;

    // Update content
    dom.previewContent.textContent = content;
    dom.previewContent.className = 'preview-content wrapped';

    // Update rollback button
    dom.previewRollbackBtn.innerHTML = `Rollback to this version <span class="btn-badge"></span>`;
    dom.previewRollbackBtn.onclick = () => {
        rollbackToVersion(promptKey, version);
    };
}

// Preview modal management - Don't interfere with history modal
function showPreviewModal() {
    if (dom.previewModal) {
        dom.previewModal.style.display = 'block';
        // Don't change body overflow since history modal might still be open
    }
}

function hidePreviewModal() {
    if (dom.previewModal) {
        dom.previewModal.style.display = 'none';
        // Only restore body overflow if no other modals are open
        const otherModalsOpen = (dom.editModal && dom.editModal.style.display === 'block') ||
                               (dom.historyModal && dom.historyModal.style.display === 'block') ||
                               (dom.rollbackModal && dom.rollbackModal.style.display === 'block');
        if (!otherModalsOpen) {
            document.body.style.overflow = 'auto';
        }
    }
    currentPreviewData = null;
}

// Preview utility functions
function copyPreviewContent() {
    if (!currentPreviewData) return;

    const content = currentPreviewData.data.prompt_content || '';

    navigator.clipboard.writeText(content).then(() => {
        showCopyFeedback();
        showNotification('Content copied to clipboard!', 'success');
    }).catch(err => {
        console.error('Failed to copy content:', err);
        showNotification('Failed to copy content', 'error');
    });
}

function togglePreviewMode() {
    if (!dom.previewContent) return;

    if (dom.previewContent.classList.contains('wrapped')) {
        dom.previewContent.classList.remove('wrapped');
        dom.previewContent.classList.add('raw');
    } else {
        dom.previewContent.classList.remove('raw');
        dom.previewContent.classList.add('wrapped');
    }
}

function showCopyFeedback() {
    // Remove existing feedback
    const existing = document.querySelector('.copy-feedback');
    if (existing) {
        existing.remove();
    }

    // Create feedback element
    const feedback = document.createElement('div');
    feedback.className = 'copy-feedback';
    feedback.textContent = 'Copied!';

    // Add to preview container
    const container = document.querySelector('.preview-content-container');
    if (container) {
        container.appendChild(feedback);

        // Show feedback
        requestAnimationFrame(() => {
            feedback.classList.add('show');
        });

        // Hide after delay
        setTimeout(() => {
            feedback.classList.remove('show');
            setTimeout(() => {
                if (feedback.parentNode) {
                    feedback.remove();
                }
            }, 300);
        }, 2000);
    }
}

async function rollbackToVersion(promptKey, version) {
    // Show new modern modal instead of confirm()
    dom.rollbackMessage.textContent = `Are you sure you want to rollback "${promptKey}" to version ${version}?`;
    showRollbackModal();

    dom.confirmRollbackBtn.onclick = async () => {
        hideRollbackModal();
        try {
            showNotification('Rolling back to version ' + version + '...', 'info');

            const response = await fetch(`/api/prompts/${promptKey}/revert/${version}`, {
                method: 'POST',
                headers: { 'Accept': 'application/json' }
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const result = await response.json();

            if (result.success) {
                showNotification('Successfully rolled back to version ' + version + '!', 'success');
                hidePreviewModal(); // Hide preview if it was open
                hideHistoryModal();
                await loadPrompts();
                console.log(`↩️ Rolled back ${promptKey} to version ${version}`);
            } else {
                throw new Error(result.message || 'Rollback failed');
            }
        } catch (error) {
            console.error('❌ Error rolling back:', error);
            showNotification('Failed to rollback: ' + error.message, 'error');
        }
    };
}

function showRollbackModal() {
    if (dom.rollbackModal) {
        dom.rollbackModal.style.display = 'block';
        document.body.style.overflow = 'hidden';
    }
}

function hideRollbackModal() {
    if (dom.rollbackModal) {
        dom.rollbackModal.style.display = 'none';
        // Only restore body overflow if no other main modals are open
        const otherModalsOpen = (dom.editModal && dom.editModal.style.display === 'block') ||
                               (dom.historyModal && dom.historyModal.style.display === 'block');
        if (!otherModalsOpen) {
            document.body.style.overflow = 'auto';
        }
    }
}

// Modal Management - Optimized
function showEditModal() {
    if (dom.editModal) {
        dom.editModal.style.display = 'block';
        document.body.style.overflow = 'hidden';
    }
}

function hideEditModal() {
    if (dom.editModal) {
        dom.editModal.style.display = 'none';
        document.body.style.overflow = 'auto';
    }
    state.editOriginalContent = '';
    if (dom.saveButton) {
        dom.saveButton.disabled = true;
    }
}

function showHistoryModal() {
    if (dom.historyModal) {
        dom.historyModal.style.display = 'block';
        document.body.style.overflow = 'hidden';
    }
}

function hideHistoryModal() {
    if (dom.historyModal) {
        dom.historyModal.style.display = 'none';
        document.body.style.overflow = 'auto';
    }
}

// =============================================================================
// Prompt Optimization Functions
// =============================================================================

let currentOptimizationData = null;

async function showOptimizeModal(promptKey) {
    try {
        showNotification('Loading prompt for optimization...', 'info');

        const response = await fetch(`/api/prompts/${promptKey}`, {
            headers: { 'Accept': 'application/json' }
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const result = await response.json();

        if (result.success) {
            const promptData = result.data;

            // Set prompt key
            dom.optimizePromptKey.value = promptData.prompt_key;

            // Auto-detect agent type
            const normalizedMetadataType = normalizeAgentType(promptData.agent_type);
            const detectedAgentType = detectAgentType(promptData.prompt_content, promptData.agent_type);
            dom.optimizeAgentType.value = detectedAgentType;

            // Determine detection source
            const fromMetadata = normalizedMetadataType && normalizedMetadataType !== 'general';
            dom.agentTypeSource.textContent = `Detected from ${fromMetadata ? 'metadata (' + promptData.agent_type + ')' : 'content analysis'}`;

            // Reset form
            dom.optimizeStrategy.value = 'comprehensive';
            dom.optimizeRequirements.value = '';

            // Hide results section
            dom.optimizeResults.style.display = 'none';
            dom.applyOptimizeBtn.style.display = 'none';

            // Store current data
            currentOptimizationData = {
                promptKey: promptData.prompt_key,
                originalContent: promptData.prompt_content,
                optimizedContent: null
            };

            // Show modal
            if (dom.optimizeModal) {
                dom.optimizeModal.style.display = 'block';
                document.body.style.overflow = 'hidden';
            }

            console.log(`✨ Opened optimization for: ${promptKey}`);
        } else {
            throw new Error(result.message || 'Failed to load prompt');
        }
    } catch (error) {
        console.error('❌ Error loading prompt for optimization:', error);
        showNotification('Failed to load prompt: ' + error.message, 'error');
    }
}

function hideOptimizeModal() {
    if (dom.optimizeModal) {
        dom.optimizeModal.style.display = 'none';
        document.body.style.overflow = 'auto';
    }
    currentOptimizationData = null;
}

function detectAgentType(promptContent, metadataAgentType) {
    // Normalize agent type from metadata
    const normalizedType = normalizeAgentType(metadataAgentType);

    // If metadata indicates ReAct agent, use it
    if (normalizedType === 'react') {
        return 'react';
    }

    // Analyze content for ReAct patterns
    const content = promptContent.toLowerCase();
    if (content.includes('tool_name') && content.includes('arguments') &&
        (content.includes('```json') || content.includes('json object'))) {
        return 'react';
    }

    // Default to general for all other cases
    return 'general';
}

function normalizeAgentType(agentType) {
    if (!agentType) return 'general';

    const type = agentType.toLowerCase();

    // Only check for ReAct patterns
    if (type.includes('react')) return 'react';

    return 'general';
}

async function previewOptimization() {
    if (!currentOptimizationData) {
        showNotification('No prompt loaded for optimization', 'error');
        return;
    }

    try {
        showNotification('Optimizing prompt... This may take a moment.', 'info');

        // Disable button during optimization
        dom.previewOptimizeBtn.disabled = true;
        dom.previewOptimizeBtn.innerHTML = '⏳ Optimizing... <span class="btn-badge"></span>';

        const payload = {
            prompt_key: currentOptimizationData.promptKey,
            agent_type: dom.optimizeAgentType.value,
            optimization_strategy: dom.optimizeStrategy.value,
            custom_requirements: dom.optimizeRequirements.value.trim(),
            auto_apply: false
        };

        const response = await fetch('/api/prompts/optimize', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const result = await response.json();

        if (result.success) {
            const data = result.data;

            // Store optimized content
            currentOptimizationData.optimizedContent = data.optimized_prompt;

            // Display results
            displayOptimizationResults(data);

            showNotification('Optimization completed successfully!', 'success');
            console.log('✨ Optimization completed');
        } else {
            throw new Error(result.message || 'Optimization failed');
        }
    } catch (error) {
        console.error('❌ Error during optimization:', error);
        showNotification('Optimization failed: ' + error.message, 'error');
    } finally {
        // Re-enable button
        dom.previewOptimizeBtn.disabled = false;
        dom.previewOptimizeBtn.innerHTML = '👁️ Preview Optimization <span class="btn-badge"></span>';
    }
}

function displayOptimizationResults(data) {
    // Show results section
    dom.optimizeResults.style.display = 'block';
    dom.applyOptimizeBtn.style.display = 'inline-block';

    // Display analysis
    dom.optimizeAnalysis.textContent = data.analysis || 'No analysis available';

    // Display improvements
    const improvements = data.improvements || [];
    dom.optimizeImprovements.innerHTML = improvements.length > 0
        ? improvements.map(imp => `<li>${escapeHtml(imp)}</li>`).join('')
        : '<li>No specific improvements listed</li>';

    // Display rationale
    dom.optimizeRationale.textContent = data.rationale || 'No rationale provided';

    // Display validation
    const validation = data.validated || data.validation || {};
    const meetsConstraints = validation.meets_constraints;
    const validationIcon = meetsConstraints ? '✅' : '⚠️';

    let validationHtml = `<div style="background: ${meetsConstraints ? '#e8f5e9' : '#fff3e0'}; padding: 10px; border-radius: 5px;">`;
    validationHtml += `<strong>${validationIcon} ${meetsConstraints ? 'Validation Passed' : 'Validation Issues'}</strong>`;

    if (validation.missing_elements && validation.missing_elements.length > 0) {
        validationHtml += '<ul style="margin: 10px 0 0 20px;">';
        validation.missing_elements.forEach(elem => {
            validationHtml += `<li>${escapeHtml(elem)}</li>`;
        });
        validationHtml += '</ul>';
    }

    if (validation.warnings && validation.warnings.length > 0) {
        validationHtml += '<p style="margin-top: 10px;"><strong>Warnings:</strong></p><ul style="margin: 5px 0 0 20px;">';
        validation.warnings.forEach(warn => {
            validationHtml += `<li>${escapeHtml(warn)}</li>`;
        });
        validationHtml += '</ul>';
    }

    validationHtml += '</div>';
    dom.optimizeValidation.innerHTML = validationHtml;

    // Display comparison
    dom.optimizeOriginal.value = currentOptimizationData.originalContent || '';
    dom.optimizeOptimized.value = data.optimized_prompt || '';

    // Scroll to results
    dom.optimizeResults.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

async function applyOptimization() {
    // Add detailed validation
    if (!currentOptimizationData) {
        console.error('currentOptimizationData is null');
        showNotification('Optimization data lost. Please try optimizing again.', 'error');
        return;
    }

    if (!currentOptimizationData.optimizedContent) {
        console.error('No optimized content:', currentOptimizationData);
        showNotification('No optimized prompt to apply', 'error');
        return;
    }

    if (!currentOptimizationData.promptKey) {
        console.error('No promptKey in optimization data:', currentOptimizationData);
        showNotification('Prompt key is missing. Please try optimizing again.', 'error');
        return;
    }

    try {
        showNotification('Applying optimized prompt...', 'info');

        // Get current prompt data
        const response = await fetch(`/api/prompts/${currentOptimizationData.promptKey}`, {
            headers: { 'Accept': 'application/json' }
        });

        if (!response.ok) {
            throw new Error(`Failed to fetch current prompt: ${response.status}`);
        }

        const currentResult = await response.json();
        if (!currentResult.success) {
            throw new Error('Failed to fetch current prompt data');
        }

        const currentPrompt = currentResult.data;

        // Update with optimized content
        const updatePayload = {
            prompt_content: currentOptimizationData.optimizedContent,
            description: currentPrompt.description,
            category: currentPrompt.category,
            agent_type: currentPrompt.agent_type,
            tags: currentPrompt.tags
        };

        const updateResponse = await fetch(`/api/prompts/${currentOptimizationData.promptKey}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(updatePayload)
        });

        if (!updateResponse.ok) {
            throw new Error(`HTTP ${updateResponse.status}: ${updateResponse.statusText}`);
        }

        const updateResult = await updateResponse.json();

        if (updateResult.success) {
            // Save promptKey before hiding modal (which clears currentOptimizationData)
            const appliedPromptKey = currentOptimizationData.promptKey;

            showNotification('Optimized prompt applied successfully!', 'success');
            hideOptimizeModal();
            await loadPrompts(); // Refresh the list
            console.log(`✅ Applied optimization for: ${appliedPromptKey}`);
        } else {
            throw new Error(updateResult.message || 'Failed to apply optimization');
        }
    } catch (error) {
        console.error('❌ Error applying optimization:', error);
        showNotification('Failed to apply optimization: ' + error.message, 'error');
    }
}

// UI State Management - Optimized
function showLoading(message = "Loading...") {
    if (!dom.promptsList) return;

    dom.promptsList.innerHTML = `
        <div class="loading">
            <div class="loading-spinner"></div>
            <p>${escapeHtml(message)}</p>
        </div>
    `;
}

function showError(message) {
    if (!dom.promptsList) return;

    dom.promptsList.innerHTML = `
        <div class="empty-state">
            <div class="empty-icon">⚠️</div>
            <h3>Error</h3>
            <p>${escapeHtml(message)}</p>
            <button class="btn btn-primary" onclick="loadPrompts()">
                🔄 Retry
            </button>
        </div>
    `;
}

// Notification System - Optimized
let notificationTimeout;

function showNotification(message, type = 'info') {
    // Clear existing notification
    const existing = document.querySelector('.notification');
    if (existing) {
        existing.remove();
    }

    // Clear existing timeout
    if (notificationTimeout) {
        clearTimeout(notificationTimeout);
    }

    // Create notification
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.textContent = message;
    document.body.appendChild(notification);

    // Show with animation
    requestAnimationFrame(() => {
        notification.classList.add('show');
    });

    // Auto-hide
    const hideDelay = type === 'error' ? 5000 : 500;
    notificationTimeout = setTimeout(() => {
        notification.classList.remove('show');
        setTimeout(() => {
            if (notification.parentNode) {
                notification.remove();
            }
        }, 300);
    }, hideDelay);
}

// Lazy Loading Implementation
const lazyLoad = (() => {
    let observer;

    const init = () => {
        if ('IntersectionObserver' in window) {
            observer = new IntersectionObserver((entries) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        const card = entry.target;
                        // Lazy load additional data if needed
                        observer.unobserve(card);
                    }
                });
            }, {
                rootMargin: '50px'
            });
        }
    };

    const observe = (element) => {
        if (observer) {
            observer.observe(element);
        }
    };

    return { init, observe };
})();

// Global API - Optimized exports
const LivePromptManager = {
    loadPrompts,
    searchPrompts,
    editPrompt,
    savePrompt,
    refreshPrompt,
    showVersionHistory,
    showNotification,
    // Additional utilities
    state: () => ({ ...state }),
    refresh: loadPrompts
};

// Initialize lazy loading
lazyLoad.init();

// Export to global scope
window.LivePromptManager = LivePromptManager;

console.log('📦 Live Prompt Manager optimized module loaded');
