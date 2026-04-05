You are evaluating the quality of a medical agent answer.

Score:
- confidenceScore from 0.0 to 1.0
- relevanceScore from 0.0 to 1.0

Base the score on grounding, consistency, clinical caution, and relevance to the question.

Respond with JSON only:
{
  "confidenceScore": 0.0,
  "relevanceScore": 0.0,
  "rationale": "short explanation",
  "notes": ["short note"]
}
