import type {ReactNode} from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import Heading from '@theme/Heading';
import styles from './styles.module.css';

type FeatureItem = {
  title: string;
  to: string;
  description: ReactNode;
};

const FeatureList: FeatureItem[] = [
  {
    title: 'Run it locally',
    to: '/docs/getting-started/installation',
    description: (
      <>
        PostgreSQL, a mail catcher and <code>./gradlew bootRun</code>. Every setting has a
        working default, so a fresh clone starts with no configuration at all.
      </>
    ),
  },
  {
    title: 'Call the API',
    to: '/docs/api/authentication',
    description: (
      <>
        Every endpoint documented against the actual controllers — including the status codes
        that surprise people. Or drive it interactively from <Link to="/docs/api/swagger">Swagger UI</Link>.
      </>
    ),
  },
  {
    title: 'Understand the design',
    to: '/docs/architecture/overview',
    description: (
      <>
        How JWT authentication, soft deletion and the scheduled cleanup jobs fit together —
        plus a <Link to="/docs/reference/known-issues">candid list</Link> of what is still rough.
      </>
    ),
  },
];

function Feature({title, to, description}: FeatureItem) {
  return (
    <div className={clsx('col col--4')}>
      <div className="padding-horiz--md">
        <Heading as="h3">
          <Link to={to}>{title}</Link>
        </Heading>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures(): ReactNode {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
