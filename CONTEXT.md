# VoiceSlip Context

VoiceSlip is an Android dictation pipeline for turning captured speech into insertable text through transcription, optional cleanup, style rules, and app insertion.

## Language

**Dictation Pipeline**:
The ordered path that captured audio follows to become final insertable text.
_Avoid_: workflow, flow

**Pipeline Preview**:
A user-facing inspection of the selected **Dictation Pipeline** that must distinguish model input from explanatory metadata.
_Avoid_: prompt preview when referring to the whole dialog

**Language Preservation**:
The requirement that generated output remains in the spoken or detected language instead of being translated.
_Avoid_: language hinting

**Language Hint**:
A user-provided language name used to strengthen **Language Preservation** prompts for audio input.
_Avoid_: detected language

**Model Picker**:
A role-specific model selection screen where a user chooses the concrete model for one step of the **Dictation Pipeline**.
_Avoid_: model manager when the primary action is selection

**Transcription Model**:
The user-facing model choice that turns audio into a raw transcript in a **Dictation Pipeline**.
_Avoid_: transcription engine in UI labels

**Post-Processing Model**:
The user-facing model choice that rewrites a raw transcript into final insertable text in a **Dictation Pipeline**.
_Avoid_: post-processing provider as the primary UI choice

**Audio Direct Model**:
The user-facing model choice that turns audio directly into final insertable text without a separate post-processing step.
_Avoid_: audio direct engine in UI labels

**Provider Group**:
A grouping inside a **Model Picker** that separates model catalogs by provider while keeping provider choice subordinate to the concrete model choice.
_Avoid_: provider setting when referring to model selection

**Favorite Model**:
A model marked for easier rediscovery inside a provider catalog, without making it the active pipeline model.
_Avoid_: selected model

**Predicted Route**:
The provider endpoint VoiceSlip expects OpenRouter to prefer for a model under an explicit routing policy, shown for comparison but not guaranteed at request time.
_Avoid_: selected provider

**Estimated Fastest Route**:
An experimental OpenRouter provider endpoint choice that would estimate response time from p50 time to first token plus expected output tokens divided by p50 throughput.
_Avoid_: smart route when the rule should be measurable

**OpenRouter Model Reasoning Effort**:
The per-model-slot reasoning effort VoiceSlip sends with an OpenRouter model request when that selected model supports reasoning.
_Avoid_: provider reasoning setting

**Dictionary During Transcription**:
The user-facing control for whether saved dictionary entries are sent to the transcription step when the selected model supports them.
_Avoid_: dictionary routing in UI labels

**Dictionary Entry**:
A saved user phrase that should be preserved as written when it matches dictated speech.
_Avoid_: term when distinguishing saved entries from provider-specific prompt parts

**Dictionary Entry Priority**:
The user-controlled order that decides which **Dictionary Entries** are shown first and sent first when model input has limited space.
_Avoid_: alphabetical dictionary order

**Bias Token**:
A provider-safe token derived from a **Dictionary Entry** for Mistral `context_bias`.
_Avoid_: dictionary entry when referring to split `context_bias` inputs

**Cleanup Policy**:
The shared prompt policy that converts dictated speech artifacts into final insertable text before style-specific wording is applied.
_Avoid_: style prompt when referring to global cleanup rules

**Dictated Structure**:
The explicit organization spoken by the user, such as steps, requirements, tasks, or topic shifts, that should be preserved as paragraphs or lists.
_Avoid_: markdown formatting when the structure is the domain concept

**Spoken Punctuation**:
Punctuation words that may represent formatting commands in dictated speech.
_Avoid_: literal words when the speaker clearly intended punctuation

**Style Preset**:
A built-in formatting and tone constraint applied after the **Cleanup Policy** preserves dictated meaning, wording, vocabulary, and structure.
_Avoid_: rewrite instruction for built-in presets

**Floating Bubble Placement**:
The saved user intent for where the floating dictation bubble belongs, expressed relative to a screen edge and usable vertical space.
_Avoid_: raw bubble coordinates when describing persisted placement

**Push to Talk**:
A secondary floating bubble gesture where holding the bubble records immediately and releasing it submits the dictation without a confirmation step.
_Avoid_: push-to-talk mode

**Submitted Dictation**:
A recording that has been accepted for transcription, cleanup, and insertion but has not yet reached a final success, copy, cancellation, or failure outcome.
_Avoid_: recording when the capture has already stopped

**Retained Dictation**:
A completed or failed dictation that remains visible in History with its recording file available for review or retry.
_Avoid_: history item when distinguishing it from abandoned captures

**Retried Dictation**:
A retained recording that is submitted again through the current pipeline settings while preserving the originally resolved writing style.
_Avoid_: replayed dictation when implying old settings should be preserved

**Untrusted Dictation Boundary**:
The fixed prompt structure that marks dictated transcript or audio content as data to transform rather than instructions to obey.
_Avoid_: cleanup rule

**Target App**:
The high-confidence app context that owns the editable field VoiceSlip is recording for.
_Avoid_: last foreground app when the value is stale or inferred

**Accessibility Setup Status**:
Whether Android reports the VoiceSlip accessibility service as enabled or the service is connected, independent of whether the bubble is visible in the current app.
_Avoid_: bubble status

**Secret Field**:
An editor whose contents are credentials or similarly secret input that VoiceSlip should not expose for dictation.
_Avoid_: sensitive field when referring to passwords, PINs, OTPs, CVVs, or card numbers

**Private Editor**:
An editor that asks keyboards not to learn from input but is not necessarily a **Secret Field**.
_Avoid_: sensitive field when referring to browser address bars or no-personalized-learning editors

## Relationships

- A **Dictation Pipeline** has one or more model steps.
- A **Transcription Model**, **Post-Processing Model**, or **Audio Direct Model** can occupy a model step in a **Dictation Pipeline**.
- A **Pipeline Preview** may show model input, but explanatory metadata is not itself model input.
- **Language Hints** support **Language Preservation** for audio prompts.
- A detected language can support **Language Preservation** after transcription, but it is distinct from a **Language Hint**.
- A **Model Picker** chooses one model for one **Dictation Pipeline** step.
- A **Provider Group** belongs inside a **Model Picker**; it is not a separate pipeline decision.
- OpenRouter provider sorting is configured inside an OpenRouter **Provider Group** and applies to all OpenRouter requests, but remains a routing policy rather than part of the selected model identity.
- OpenRouter throughput routing is the default routing policy and should be visually recommended in the routing selector, not in per-model route summaries.
- OpenRouter settings inside a **Provider Group** should only configure shared OpenRouter provider routing, not per-slot reasoning.
- **OpenRouter Model Reasoning Effort** is stored per OpenRouter model slot in the **Dictation Pipeline**, so transcription, post-processing, and audio-direct selections can use different reasoning levels.
- **OpenRouter Model Reasoning Effort** belongs to the same persisted pipeline configuration as the selected OpenRouter model ID, not to shared provider preferences.
- **OpenRouter Model Reasoning Effort** is presented during model selection only when OpenRouter catalog metadata says the selected model supports reasoning.
- Selecting a reasoning-capable OpenRouter model asks for **OpenRouter Model Reasoning Effort** even when the same model is already selected, because the user may be changing the slot's reasoning level.
- The default **OpenRouter Model Reasoning Effort** is None; UI guidance should describe lower reasoning as faster for the current model step rather than naming a specific pipeline stage.
- Auto **OpenRouter Model Reasoning Effort** means no reasoning override is sent; None sends an explicit request to disable reasoning.
- Every explicit **OpenRouter Model Reasoning Effort** request includes reasoning exclusion so reasoning text cannot become dictation output; Auto omits the reasoning object entirely.
- **OpenRouter Model Reasoning Effort** choices should be shown in one compact selector with None first, followed by Minimal, Low, Medium, High, XHigh, and Auto.
- Selected OpenRouter model summaries should show **OpenRouter Model Reasoning Effort** only when it is meaningful, using a compact reasoning indicator so values like None are not ambiguous.
- Selected model summaries should prefer meaningful model display names while keeping exact raw model IDs available in picker rows or detail sheets.
- Selecting a model in a **Model Picker** should update the selection in place rather than closing the picker.
- The in-place selection behavior, pinned selected section, and duplicate exclusion apply to all **Model Pickers**, while inline reasoning controls apply only to reasoning-capable OpenRouter models.
- For reasoning-capable OpenRouter models, tapping the model selects it immediately with default None when needed, and **OpenRouter Model Reasoning Effort** is edited inline inside the **Model Picker**.
- In a **Model Picker**, the selected model should appear in a compact pinned selected section directly above the normal model list.
- The pinned selected section in a **Model Picker** should remain visible even when the search query does not match it.
- The pinned selected row should keep the normal row actions such as details and favorite toggling.
- The inline **OpenRouter Model Reasoning Effort** panel should stay visible for the selected reasoning-capable OpenRouter model.
- Tapping an inline **OpenRouter Model Reasoning Effort** chip updates the selected model slot immediately.
- The inline **OpenRouter Model Reasoning Effort** panel should feel attached to the selected model row and stay compact enough not to dominate the picker.
- None should be visually recommended in the inline **OpenRouter Model Reasoning Effort** controls.
- The normal **Model Picker** list should exclude the model shown in the pinned selected section.
- If selecting a model changes the pinned selected section, the picker should scroll upward only when the selected row would otherwise be above or clipped above the visible viewport.
- Model catalog refresh controls should be integrated compactly with model search rather than taking a full-width action row.
- **Model Picker** headers should use compact navigation controls and leave enough title space for role-specific picker names.
- Shared OpenRouter settings should be reachable from every OpenRouter **Provider Group** where OpenRouter models can be chosen.
- Model rows inside a single active **Provider Group** should not repeat that provider name unless they need to distinguish an inactive saved model.
- Selecting a model in a **Model Picker** changes the active model for that pipeline step; toggling a **Favorite Model** does not.
- A **Predicted Route** may summarize p50 price, throughput, and latency for OpenRouter model rows when an explicit OpenRouter provider sort is selected.
- An **Estimated Fastest Route** is a deferred future routing policy, not part of the current OpenRouter routing controls.
- When OpenRouter default routing is selected, VoiceSlip should show model-level lowest price rather than guessing a **Predicted Route**.
- OpenRouter route performance summaries use p50 metrics; missing route metrics should be shown as unavailable rather than hidden.
- OpenRouter endpoint details should be cached separately from model catalogs because endpoint metadata is fetched per model and has different freshness.
- OpenRouter endpoint details with no endpoint candidates should first show a compact empty state and avoid metric explanations when no metrics are visible.
- OpenRouter reasoning output should be excluded from model responses so reasoning text does not become insertable dictation output.
- In compact model rows with both details and favorite actions, the details action appears left of the star and the star remains the far-right action.
- Model details should open from an explicit details action, not from long-press or row expansion, and should appear as a bottom sheet only when meaningful provider metadata is available.
- **Favorite Models** are scoped to a provider catalog and role family: OpenRouter audio favorites are shared by transcription and audio-direct model pickers, while post-processing favorites are separate for Groq and OpenRouter.
- **Dictionary During Transcription** may transform **Dictionary Entries** into provider-specific **Bias Tokens** before sending them to a transcription provider.
- Cleanup always receives the full set of **Dictionary Entries**, regardless of **Dictionary During Transcription**.
- **Dictionary Entry Priority** controls dictionary display order and the order in which dictionary prompt constraints are built.
- The **Cleanup Policy** preserves **Dictated Structure** before style prompts adjust tone and punctuation.
- The **Cleanup Policy** converts **Spoken Punctuation** only when the words function as formatting instructions in context.
- The **Untrusted Dictation Boundary** is app infrastructure and should not be overridable by **Cleanup Policy** or **Style Preset** customization.
- A **Style Preset** constrains formatting and tone; it should not paraphrase dictated wording unless a user-authored custom style explicitly asks for that.
- A **Style Preset** may adjust **Dictionary Entry** casing when casing is part of the preset, but it should otherwise preserve saved spellings.
- The Formal **Style Preset** may lightly improve grammar, punctuation, and register, but should not replace the user's vocabulary to sound more formal.
- Built-in **Style Preset** default changes should be versioned while preserving user-authored overrides.
- **Floating Bubble Placement** should survive screen rotation by preserving edge affinity and relative vertical position, not absolute pixels.
- **Push to Talk** coexists with tap-to-record; it does not replace the confirmed recording interaction.
- **Push to Talk** should start only after an intentional hold; accidental near-immediate releases should not create a submitted dictation.
- **Push to Talk** has no explicit cancel control once active; releasing submits unless the recording was below the accidental-hold threshold.
- A **Submitted Dictation** keeps the floating bubble visible until transcription, cleanup, and insertion or copy have finished.
- A new dictation should be visible at the top of History when VoiceSlip is reopened, even before it becomes a **Retained Dictation**.
- A canceled or accidental capture is not a **Retained Dictation** and should be removed from History while its recording file is deleted.
- Once setup is complete, History is the primary review surface for **Retained Dictations** when opening VoiceSlip.
- A **Retried Dictation** should use the current **Dictation Pipeline** and model settings, but preserve the originally resolved style because retry is triggered from the VoiceSlip UI rather than the original **Target App**.
- Orphaned recording files that are not referenced by **Retained Dictations** should be cleaned up after they are old enough to avoid active-recording races.
- Screen-awake behavior belongs to active audio capture, not the later **Submitted Dictation** processing window.
- A **Target App** may resolve from the focused application window, active root, focused editable node, or input editor package; if none is high-confidence, VoiceSlip should treat it as unknown.
- **Accessibility Setup Status** does not imply that the bubble is visible, because VoiceSlip may intentionally hide the bubble in its own app or unsuitable fields.
- A **Secret Field** always prevents VoiceSlip from showing the floating bubble or inserting dictated text.
- A **Private Editor** does not hide the floating bubble or block insertion by itself.

## Example dialogue

> **Dev:** "The **Pipeline Preview** shows `Language: omitted`; is that sent to the model?"
> **Domain expert:** "No. If **Language Preservation** is off, no language block should be sent, and the preview should not present omitted metadata as model input."

> **Dev:** "Should users choose Groq first and then choose a post-processing model?"
> **Domain expert:** "No. The user chooses the concrete model in a **Model Picker**; Groq and OpenRouter are **Provider Groups** inside that choice."

> **Dev:** "Should **Private Editors** be treated as sensitive fields?"
> **Domain expert:** "No. Only **Secret Fields** are always blocked; **Private Editors** should still allow dictation."

## Flagged ambiguities

- "Pipeline preview" was used to mean both the whole inspection dialog and the exact prompt sent to a model. Resolved: **Pipeline Preview** is the whole dialog; model input must be labeled as prompt or system/user prompt.
- "Manage OpenRouter audio models" was used for both selecting an active model and maintaining favorites. Resolved: active model selection belongs in a **Model Picker**; favorites are supporting controls inside the OpenRouter **Provider Group**.
- "Sort OpenRouter models by price, throughput, or latency" could imply changing model list order. Resolved: the setting controls OpenRouter provider routing for requests; model list display may show metadata but model identity stays unchanged.
- "Provider that would be selected" sounded deterministic. Resolved: use **Predicted Route** only for explicit OpenRouter routing policies because OpenRouter may still reroute at request time; do not predict default routing.
- "OpenRouter · model-id" row subtitles repeated the active **Provider Group** and caused avoidable wrapping. Resolved: omit the provider name in active provider-group rows and reserve it for inactive saved-model labels.
- "Long-press for model details" was discoverable only by convention and expanded dense picker rows. Resolved: use an explicit details action that opens a dialog or sheet.
- "Clicking a model" was ambiguous between inspecting, favoriting, and selecting. Resolved: tapping the model row selects it and returns; tapping the star only toggles **Favorite Model** state.
- "Dictionary routing" sounded like the dictionary might be routed away from cleanup. Resolved: UI should say **Dictionary During Transcription**; cleanup always uses all **Dictionary Entries**.
- "Terms included" was ambiguous after Mistral splits multi-word entries. Resolved: distinguish saved **Dictionary Entries** from provider-specific **Bias Tokens**.
- "Sorting dictionary terms" could mean alphabetical display or model priority. Resolved: **Dictionary Entry Priority** is user-controlled order and replaces alphabetical order as the primary dictionary order.
- "Create a list" could imply inventing structure. Resolved: **Dictated Structure** should be rendered when clearly spoken, while preserving the user's lead-in wording where possible.
- "Question mark" could be literal sentence content or **Spoken Punctuation**. Resolved: convert it only when context shows it is a formatting instruction.
- "Casual style" could imply paraphrasing into more casual vocabulary. Resolved: built-in **Style Presets** are formatting and tone constraints, not paraphrasing instructions.
- "Bubble position" could mean live overlay pixels or saved user placement. Resolved: **Floating Bubble Placement** is persisted as edge-relative intent; raw coordinates are only a rendering detail.
- "Push to talk" could imply a separate recording mode. Resolved: **Push to Talk** is a secondary hold gesture available alongside tap-to-record.
- "Default place" could mean always opening settings or always opening recent output. Resolved: after setup is complete, VoiceSlip should open on History; incomplete setup still opens on Setup.
- "New history entry" could mean a completed result or any newly visible dictation. Resolved: a dictation counts as new by id when it first appears in History; later status updates to that same id should not force another scroll.
- "Unknown app" could be resolved using the last seen foreground app. Rejected: **Target App** must come from high-confidence current editor/window signals to avoid applying the wrong style.
- "Sensitive field" was used for both password-class editors and no-personalized-learning editors. Resolved: use **Secret Field** for password/PIN/OTP/CVV/card input and **Private Editor** for no-personalized-learning fields such as browser address bars.
