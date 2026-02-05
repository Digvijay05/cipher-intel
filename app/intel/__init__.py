"""Intelligence extraction package.

Exports extract_intelligence and ExtractedIntelligence.
"""

from app.intel.extractor import (
    ExtractedIntelligence,
    extract_intelligence,
    merge_intel_buffer,
)

__all__ = ["ExtractedIntelligence", "extract_intelligence", "merge_intel_buffer"]
