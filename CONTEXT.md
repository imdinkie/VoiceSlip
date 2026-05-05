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

## Relationships

- A **Dictation Pipeline** has one or more model steps.
- A **Pipeline Preview** may show model input, but explanatory metadata is not itself model input.
- **Language Hints** support **Language Preservation** for audio prompts.
- A detected language can support **Language Preservation** after transcription, but it is distinct from a **Language Hint**.

## Example dialogue

> **Dev:** "The **Pipeline Preview** shows `Language: omitted`; is that sent to the model?"
> **Domain expert:** "No. If **Language Preservation** is off, no language block should be sent, and the preview should not present omitted metadata as model input."

## Flagged ambiguities

- "Pipeline preview" was used to mean both the whole inspection dialog and the exact prompt sent to a model. Resolved: **Pipeline Preview** is the whole dialog; model input must be labeled as prompt or system/user prompt.
