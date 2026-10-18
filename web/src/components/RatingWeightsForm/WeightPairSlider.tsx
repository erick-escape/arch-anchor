import { Slider, Typography } from 'antd';
import { WeightPair } from '../../interface/RatingWeights';
import styles from '../SettingsModal/Settings.module.css';
import { Meter } from '../vocabulary/Meter';
import { pairFromSimilarityWeight } from './weightPairs';

interface WeightPairSliderProps {
  // Also names the slider handle: "<name>: similarity weight".
  name: string;
  explanation: string;
  pair: WeightPair;
  onChange: (pair: WeightPair) => void;
}

// One slider per pair: the handle sets the similarity weight, violations get the rest.
const SLIDER_MARKS = { 0: 'Violations', 0.5: 'Equal', 1: 'Similarity' };

// The end marks are centred on the ends of the rail, so half of each label would stick out of
// the dialog's content without this inset.
const SLIDER_STYLE = { marginInline: 36 };

const WeightPairSlider = ({ name, explanation, pair, onChange }: WeightPairSliderProps) => (
  <section aria-label={name} className={styles.pair}>
    <Typography.Title level={5}>{name}</Typography.Title>
    <Typography.Paragraph type="secondary">{explanation}</Typography.Paragraph>
    <Slider
      min={0}
      max={1}
      step={0.05}
      marks={SLIDER_MARKS}
      style={SLIDER_STYLE}
      value={pair.similarity}
      onChange={(similarity: number) => onChange(pairFromSimilarityWeight(similarity))}
      ariaLabelForHandle={`${name}: similarity weight`}
      tooltip={{ open: false }}
    />
    <div className={styles.weights}>
      <Meter
        label="Similarity"
        value={pair.similarity}
        format="rate"
        name={`${name}: similarity weight`}
      />
      <Meter
        label="Violations"
        value={pair.violation}
        format="rate"
        name={`${name}: violations weight`}
      />
    </div>
  </section>
);

export default WeightPairSlider;
