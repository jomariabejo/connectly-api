---
name: Feature Request
about: Propose a new feature or enhancement
title: '[FEATURE] '
labels: ['type: feature', 'status: planning']
assignees: ''
---

## 🎯 Feature Description
<!-- Brief summary of what this feature does and why it's needed -->



## 📝 User Story
**As a** [type of user],  
**I want to** [perform some action],  
**So that** [I can achieve some benefit/goal].

**Example:**
As a sari-sari store owner,  
I want to track customer credit purchases,  
So that I know who owes me money and can collect payments on time.

---

## 🏪 Business Type
<!-- Which business vertical is this feature for? Check all that apply -->
- [ ] Sari-Sari Store
- [ ] Vulcanizing Shop
- [ ] Catering Business
- [ ] All Businesses (Core Platform Feature)

---

## ✅ Acceptance Criteria
<!-- Specific, testable conditions that must be met for this feature to be considered complete -->

- [ ] Criterion 1: [Describe what must work]
- [ ] Criterion 2: [Describe another expected outcome]
- [ ] Criterion 3: [Edge case or validation rule]
- [ ] Criterion 4: [Integration or UI requirement]

**Example:**
- [ ] Owner can add a credit transaction with customer name, amount, and date
- [ ] System displays total outstanding credit per customer
- [ ] Owner can record payment (partial or full)
- [ ] Credit history is retained after payment
- [ ] Only authorized users can view/edit credit records (tenant isolation)

---

## 🖼️ UI/UX Mockups (Optional)
<!-- Add screenshots, wireframes, or describe the expected user interface -->



---

## 🔧 Technical Notes
<!-- Architecture decisions, dependencies, security considerations, database changes -->

**Dependencies:**
- Depends on: #[issue number]
- Blocks: #[issue number]

**Database Changes:**
- [ ] New tables required
- [ ] Schema modifications needed
- [ ] Migration scripts needed

**Security Considerations:**
- [ ] Requires tenant isolation (RLS policies)
- [ ] Needs role-based access control
- [ ] Input validation required
- [ ] Sensitive data handling

**API Changes:**
- [ ] New endpoints required
- [ ] Breaking changes to existing API

**Performance Considerations:**
<!-- Any concerns about scalability, caching, or optimization -->

---

## 📊 Effort Estimate
**Story Points:** [1, 2, 3, 5, 8, 13, 21]  
**Time Estimate:** [hours/days]  
**Complexity:** 
- [ ] Low - Straightforward implementation
- [ ] Medium - Some complexity or unknowns
- [ ] High - Requires research or significant refactoring

---

## 🎯 Priority
- [ ] P0: Critical - Must have for launch
- [ ] P1: High - Important for user experience
- [ ] P2: Medium - Nice to have
- [ ] P3: Low - Future enhancement

---

## 📱 Mobile Considerations
<!-- How should this work on mobile devices? -->
- [ ] Mobile-first design required
- [ ] Responsive design sufficient
- [ ] Desktop only

---

## 🔗 Related Issues
<!-- Link to related features, bugs, or discussions -->

- Related to: #[issue]
- Similar to: #[issue]
- Part of epic: #[epic issue]

---

## 💡 Additional Context
<!-- Add any other context, screenshots, research, or examples -->



---

## ✨ Success Metrics (Optional)
<!-- How will we know this feature is successful? -->

- User engagement: [target metric]
- Task completion time: [reduction goal]
- User feedback: [satisfaction target]

---

<!-- 
TIPS FOR WRITING GOOD FEATURE REQUESTS:
1. Be specific - "Add export button" beats "improve exports"
2. Focus on the problem, not the solution - let implementation be flexible
3. Include real user scenarios
4. Break large features into smaller issues
5. Link to related work
-->