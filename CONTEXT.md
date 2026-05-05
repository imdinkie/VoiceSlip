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

**Dictionary During Transcription**:
The user-facing control for whether saved dictionary entries are sent to the transcription step when the selected model supports them.
_Avoid_: dictionary routing in UI labels

**Dictionary Entry**:
A saved user phrase that should be preserved as written when it matches dictated speech.
_Avoid_: term when distinguishing saved entries from provider-specific prompt parts

**Bias Token**:
A provider-safe token derived from a **Dictionary Entry** for Mistral `context_bias`.
_Avoid_: dictionary entry when referring to split `context_bias` inputs

**Cleanup Policy**:
The shared prompt policy that converts dictated speech artifacts into final insertable text before style-specific wording is applied.
_Avoid_: style prompt when referring to global cleanup rules

**Dictated Structure**:
The explicit organization spoken by the user, such as steps, requirements, tasks, or topic shifts, that should be preserved as paragraphs or lists.
_Avoid_: markdown formatting when the structure is the domain concept

**Style Preset**:
A built-in formatting and tone constraint applied after the **Cleanup Policy** preserves dictated meaning, wording, vocabulary, and structure.
_Avoid_: rewrite instruction for built-in presets

## Relationships

- A **Dictation Pipeline** has one or more model steps.
- A **Transcription Model**, **Post-Processing Model**, or **Audio Direct Model** can occupy a model step in a **Dictation Pipeline**.
- A **Pipeline Preview** may show model input, but explanatory metadata is not itself model input.
- **Language Hints** support **Language Preservation** for audio prompts.
- A detected language can support **Language Preservation** after transcription, but it is distinct from a **Language Hint**.
- A **Model Picker** chooses one model for one **Dictation Pipeline** step.
- A **Provider Group** belongs inside a **Model Picker**; it is not a separate pipeline decision.
- Selecting a model in a **Model Picker** changes the active model for that pipeline step; toggling a **Favorite Model** does not.
- **Favorite Models** are scoped to a provider catalog and role family: OpenRouter audio favorites are shared by transcription and audio-direct model pickers, while post-processing favorites are separate for Groq and OpenRouter.
- **Dictionary During Transcription** may transform **Dictionary Entries** into provider-specific **Bias Tokens** before sending them to a transcription provider.
- Cleanup always receives the full set of **Dictionary Entries**, regardless of **Dictionary During Transcription**.
- The **Cleanup Policy** preserves **Dictated Structure** before style prompts adjust tone and punctuation.
- A **Style Preset** constrains formatting and tone; it should not paraphrase dictated wording unless a user-authored custom style explicitly asks for that.

## Example dialogue

> **Dev:** "The **Pipeline Preview** shows `Language: omitted`; is that sent to the model?"
> **Domain expert:** "No. If **Language Preservation** is off, no language block should be sent, and the preview should not present omitted metadata as model input."

> **Dev:** "Should users choose Groq first and then choose a post-processing model?"
> **Domain expert:** "No. The user chooses the concrete model in a **Model Picker**; Groq and OpenRouter are **Provider Groups** inside that choice."

## Flagged ambiguities

- "Pipeline preview" was used to mean both the whole inspection dialog and the exact prompt sent to a model. Resolved: **Pipeline Preview** is the whole dialog; model input must be labeled as prompt or system/user prompt.
- "Manage OpenRouter audio models" was used for both selecting an active model and maintaining favorites. Resolved: active model selection belongs in a **Model Picker**; favorites are supporting controls inside the OpenRouter **Provider Group**.
- "Clicking a model" was ambiguous between inspecting, favoriting, and selecting. Resolved: tapping the model row selects it and returns; tapping the star only toggles **Favorite Model** state.
- "Dictionary routing" sounded like the dictionary might be routed away from cleanup. Resolved: UI should say **Dictionary During Transcription**; cleanup always uses all **Dictionary Entries**.
- "Terms included" was ambiguous after Mistral splits multi-word entries. Resolved: distinguish saved **Dictionary Entries** from provider-specific **Bias Tokens**.
- "Create a list" could imply inventing structure. Resolved: **Dictated Structure** should be rendered when clearly spoken, while preserving the user's lead-in wording where possible.
- "Casual style" could imply paraphrasing into more casual vocabulary. Resolved: built-in **Style Presets** are formatting and tone constraints, not paraphrasing instructions.
