import { Slider, Typography } from 'antd';
import { WeightPair } from '../../interface/RatingWeights';
import { describeWeightPair, pairFromSimilarityWeight } from './weightPairs';

interface WeightPairSliderProps {
  // Also names the slider handle: "<name>: similarity weight".
  name: string;
  explanation: string;
  pair: WeightPair;
  onChange: (pair: WeightPair) => void;
}

// One slider per pair: the handle sets the similarity weight, violations get the rest.
const SLIDER_MARKS = { 0: 'Violations', 0.5: 'Equal', 1: 'Similarity' };

const WeightPairSlider = ({ name, explanation, pair, onChange }: WeightPairSliderProps) => (
  <section aria-label={name} style={{ marginBottom: 24 }}>
    <Typography.Title level={5}>{name}</Typography.Title>
    <Typography.Paragraph type="secondary">{explanation}</Typography.Paragraph>
    <Slider
      min={0}
      max={1}
      step={0.05}
      marks={SLIDER_MARKS}
      value={pair.similarity}
      onChange={(similarity: number) => onChange(pairFromSimilarityWeight(similarity))}
      ariaLabelForHandle={`${name}: similarity weight`}
      tooltip={{ open: false }}
    />
    <Typography.Text strong>{describeWeightPair(pair)}</Typography.Text>
  </section>
);

export default WeightPairSlider;
