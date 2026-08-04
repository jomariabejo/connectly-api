import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

const GITHUB_REPO = 'https://github.com/jomariabejo/connectly-api';

const config: Config = {
  title: 'Documentation Central',
  tagline: 'Everything about the Connectly API, in one place',
  favicon: 'img/favicon.ico',

  future: {
    v4: true,
  },

  // Vercel serves this site at the project root. Swap in the real domain once the
  // project is linked -- only absolute URLs (sitemap, social cards) depend on it.
  url: 'https://connectly-documentation-central.vercel.app',
  baseUrl: '/',

  organizationName: 'jomariabejo',
  projectName: 'connectly-api',

  // Fail the build rather than ship a dead cross-reference.
  onBrokenLinks: 'throw',

  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  markdown: {
    mermaid: true,
    hooks: {
      onBrokenMarkdownLinks: 'throw',
    },
  },
  themes: ['@docusaurus/theme-mermaid'],

  presets: [
    [
      'classic',
      {
        docs: {
          sidebarPath: './sidebars.ts',
          editUrl: `${GITHUB_REPO}/tree/main/documentation-central/`,
        },
        // A documentation site, not a publication.
        blog: false,
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  themeConfig: {
    image: 'img/docusaurus-social-card.jpg',
    colorMode: {
      respectPrefersColorScheme: true,
    },
    navbar: {
      title: 'Documentation Central',
      logo: {
        alt: 'Connectly API',
        src: 'img/logo.svg',
      },
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'docsSidebar',
          position: 'left',
          label: 'Docs',
        },
        {
          to: '/docs/api/authentication',
          label: 'API Reference',
          position: 'left',
        },
        {
          to: '/docs/api/swagger',
          label: 'Swagger UI',
          position: 'left',
        },
        {
          href: GITHUB_REPO,
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Get Started',
          items: [
            {label: 'Introduction', to: '/docs/intro'},
            {label: 'Installation', to: '/docs/getting-started/installation'},
            {label: 'Configuration', to: '/docs/getting-started/configuration'},
          ],
        },
        {
          title: 'API',
          items: [
            {label: 'Authentication', to: '/docs/api/authentication'},
            {label: 'Errors', to: '/docs/api/errors'},
            {label: 'Pagination', to: '/docs/api/pagination'},
            {label: 'Swagger UI', to: '/docs/api/swagger'},
          ],
        },
        {
          title: 'More',
          items: [
            {label: 'Architecture', to: '/docs/architecture/overview'},
            {label: 'Testing', to: '/docs/testing/mockito-suite'},
            {label: 'Known Issues', to: '/docs/reference/known-issues'},
            {label: 'GitHub', href: GITHUB_REPO},
          ],
        },
      ],
      copyright: `Connectly API is MIT licensed. Built with Docusaurus. Copyright © ${new Date().getFullYear()} jomariabejo.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      // None of these ship with Prism by default, and every code sample here needs one.
      additionalLanguages: ['java', 'sql', 'bash', 'properties', 'json', 'yaml', 'groovy', 'http'],
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
