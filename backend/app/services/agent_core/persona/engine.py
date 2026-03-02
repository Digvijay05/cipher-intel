"""Persona engine for dynamically loading and injecting behavioral traits."""

import os
import yaml
from typing import Dict, Any


class PersonaEngine:
    """Loads externalized YAML persona configs and generates system instruction blocks."""

    def __init__(self, template_dir: str = None):
        if template_dir is None:
            # Default to the templates directory adjacent to this file
            base_path = os.path.dirname(__file__)
            self.template_dir = os.path.join(base_path, "templates")
        else:
            self.template_dir = template_dir
            
        self._cache: Dict[str, Dict[str, Any]] = {}

    def load_persona(self, persona_id: str) -> Dict[str, Any]:
        """Load a YAML persona by ID. Caches the result."""
        if persona_id in self._cache:
            return self._cache[persona_id]

        file_path = os.path.join(self.template_dir, f"{persona_id}.yml")
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"Persona template not found: {file_path}")

        with open(file_path, "r", encoding="utf-8") as f:
            persona_data = yaml.safe_load(f)
            
        self._cache[persona_id] = persona_data
        return persona_data

    def build_system_prompt_segment(self, persona_id: str) -> str:
        """Hydrate the bare persona config into a rich LLM instructional block."""
        p = self.load_persona(persona_id)
        
        # Extrapolate sections safely
        demo = p.get("demographics", {})
        traits = p.get("traits", {})
        ling = p.get("linguistic", {})
        rules = p.get("engagement_rules", {})

        def _flatten_list(items):
            res = []
            for item in items:
                if isinstance(item, dict):
                    for k, v in item.items():
                        res.append(f"{k}: {v}")
                else:
                    res.append(str(item))
            return res

        behavioral = _flatten_list(traits.get('behavioral', []))
        biases = _flatten_list(traits.get('cognitive_biases', []))

        prompt = f"""You are {demo.get('name', 'a user')}, aged {demo.get('age', 'unknown')} from {demo.get('location', 'unknown')}.
Socioeconomic background: {demo.get('socioeconomic', 'average')}
Technical literacy: {demo.get('technical_literacy', 'average')}

# BEHAVIORAL TRAITS (CRITICAL)
- {chr(10) + '- '.join(behavioral)}

# COGNITIVE BIASES
- {chr(10) + '- '.join(biases)}

# EMOTIONAL STATE
- Baseline: {traits.get('emotional_modeling', {}).get('baseline', 'calm')}
- Under Pressure: {traits.get('emotional_modeling', {}).get('under_pressure', 'anxious')}

# LINGUISTIC STYLE
- {ling.get('style', 'casual')}
- DO NOT UNDERSTAND: {ling.get('vocabulary_limits', 'highly technical jargon')}

# CORE DIRECTIVES & RISK TOLERANCE
- {rules.get('risk_tolerance', 'moderate')}
- TACTIC: {rules.get('extraction_bait', 'ask natural questions')}
"""
        return prompt
