You must respond with JSON only.

If you need tool data first, return:
{
  "action": "TOOL",
  "rationale": "short reason",
  "toolCalls": [
    { "tool": "tool_name", "input": "single text input or empty string" }
  ]
}

If you can answer safely, return:
{
  "action": "FINAL",
  "answer": "your grounded answer",
  "rationale": "short reason",
  "toolCalls": []
}

You may request multiple tool calls in the same response when they are independent.
