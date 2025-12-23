package com.jd.oxygent.core.oxygent.samples.examples.agent;

/**
 * Parallel Demo Class
 * Demonstrates parallel agent processing for comprehensive project evaluation
 * Shows how to use multiple expert agents to analyze different aspects of a project simultaneously
 *
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */

import com.jd.oxygent.core.oxygent.oxy.BaseOxy;
import com.jd.oxygent.core.oxygent.oxy.agents.ChatAgent;
import com.jd.oxygent.core.oxygent.oxy.agents.ParallelAgent;
import com.jd.oxygent.core.oxygent.oxy.llms.HttpLlm;
import com.jd.oxygent.core.oxygent.samples.server.ServerApp;
import com.jd.oxygent.core.oxygent.samples.server.masprovider.engine.annotation.OxySpaceBean;
import com.jd.oxygent.core.oxygent.samples.server.utils.GlobalDefaultOxySpaceMapping;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ParallelDemo {
    @OxySpaceBean(value = "parallelAgentJavaOxySpace", defaultStart = true, query = """
            Project Background:
            We are a mid-sized e-commerce company with a customer service team of 50 people handling over 5,000 inquiries per day. Main types of inquiries include:
            - Order status inquiries (40%)
            - Product information (30%)
            - After-sales service (20%)
            - Other issues (10%)
            
            Project Goals:
            We aim to build an intelligent customer service system that can:
            1. Automatically handle over 80% of common questions
            2. Provide 24/7 support
            3. Reduce labor costs by over 30%
            4. Improve customer satisfaction to above 90%
            
            Current Resources:
            - Tech team: 10 members (including 2 AI engineers)
            - Budget: 2 million RMB
            - Timeline: Aim to launch MVP within 6 months
            - Data: 500,000 historical customer service records over the past 2 years
            
            Specific Requirements:
            1. Support both text and voice interactions
            2. Integrate with existing CRM and order system
            3. Support multi-turn conversations and context understanding
            4. Include human handoff mechanism
            5. Require high availability (99.9%+)
            """)
    public static List<BaseOxy> getDefaultOxySpace() {

        return Arrays.asList(
                HttpLlm.builder()
                        .name("default_llm")
                        .apiKey(EnvUtils.getEnv("GPT_LLM_API_KEY"))
                        .baseUrl(EnvUtils.getEnv("GPT_LLM_BASE_URL"))
                        .modelName(EnvUtils.getEnv("GPT_LLM_MODEL_NAME"))
                        .llmParams(Map.of("max_tokens", 8192, "temperature", 0.7))
                        .build(),
                ChatAgent.builder()
                        .name("tech_expert")
                        .llmModel("default_llm")
                        .desc("AI product technical feasibility expert")
                        .prompt("""
                                You are a senior technical architect and AI systems expert, responsible for comprehensive technical feasibility assessments.
                                
                                Analysis Framework:
                                1. **Tech Stack Evaluation**
                                   - Recommended core tech stack (NLP models, dialogue management, knowledge base, etc.)
                                   - Comparison: open source vs commercial solutions
                                   - Maturity and stability assessment
                                
                                2. **System Architecture Design**
                                   - Overall system architecture recommendations
                                   - Key technical components and module breakdown
                                   - Data flow and processing pipeline
                                   - Scalability and performance considerations
                                
                                3. **Technical Challenges & Solutions**
                                   - Identification of major technical challenges
                                   - Proposed targeted solutions
                                   - Technical risk estimation and mitigation strategies
                                
                                4. **Development Resource Estimation**
                                   - Required team structure
                                   - Estimated development timeline
                                   - Learning curve and training needs
                                
                                Please output in a structured format, including a technical feasibility conclusion (score from 1 to 10) and key recommendations."""
                        )
                        .build(),
                ChatAgent.builder()
                        .name("business_expert")
                        .llmModel("default_llm")
                        .desc("AI product business value evaluation expert")
                        .prompt("""
                                You are an experienced business analyst and product strategist, focused on assessing the business value of AI products.
                                
                                Analysis Framework:
                                1. **Market Opportunity Analysis**
                                   - Target market size and growth potential
                                   - Competitor analysis and differentiation strategy
                                   - Customer pain points and value proposition
                                
                                2. **Business Model Design**
                                   - Revenue model suggestions (SaaS, pay-per-use, etc.)
                                   - Cost structure analysis
                                   - Profitability forecast
                                
                                3. **Return on Investment Analysis**
                                   - Initial cost estimation
                                   - Expected returns and payback period
                                   - Key financial metrics (NPV, IRR, breakeven)
                                
                                4. **Implementation Strategy**
                                   - Go-to-market strategy
                                   - Customer acquisition and retention
                                   - Business growth roadmap
                                
                                5. **Key Success Factors**
                                   - Key performance indicators (KPIs)
                                   - Milestone planning
                                   - Resource allocation recommendations
                                
                                Please output in a structured format, including a business feasibility conclusion (score from 1 to 10) and key recommendations.
                                """
                        )
                        .build(),
                ChatAgent.builder()
                        .name("risk_expert")
                        .llmModel("default_llm")
                        .desc("AI project risk management expert")
                        .prompt("""
                                You are a professional risk management expert specialized in AI projects. You are only responsible for identifying, evaluating, and managing risks. Ignore all other aspects.
                                
                                **Important Constraint: Only analyze from a risk management perspective. Do not touch technical, legal, or business issues.**
                                
                                Risk Assessment Framework:
                                1. **Technical Risks**
                                   - Underperforming AI model
                                   - Unanticipated technical complexity
                                   - Third-party dependency
                                   - Data quality and acquisition risks
                                
                                2. **Market Risks**
                                   - Changing market demand
                                   - Intensifying competition
                                   - Customer adoption risk
                                   - Risk of technological substitution
                                
                                3. **Operational Risks**
                                   - Staff turnover
                                   - Project management challenges
                                   - Budget overruns
                                   - Timeline delays
                                
                                4. **Compliance & Security Risks**
                                   - Data privacy and security
                                   - AI ethics and bias
                                   - Regulatory changes
                                   - Intellectual property issues
                                
                                For each risk item, please provide:
                                - Probability (Low / Medium / High)
                                - Impact (Low / Medium / High)
                                - Risk level (Low / Medium / High / Critical)
                                - Mitigation measures
                                - Contingency plan
                                
                                Finally, provide an overall risk rating and key risk control suggestions.
                                """
                        )
                        .build(),
                ChatAgent.builder()
                        .name("legal_expert")
                        .llmModel("default_llm")
                        .desc("AI product legal compliance and IP expert")
                        .prompt("""
                                You are a professional legal expert specializing in AI-related compliance and intellectual property protection. You should ignore all non-legal aspects.
                                
                                **Important Constraint: Only analyze from a legal perspective. Do not discuss technical, business, or risk issues.**
                                
                                Compliance Analysis Framework:
                                1. **Data Compliance**
                                   - Personal Information Protection Law (PIPL) compliance
                                   - Cross-border data transfer regulations
                                   - User consent and notification mechanism
                                   - Data storage and processing standards
                                
                                2. **AI Governance Compliance**
                                   - Regulations on algorithmic recommendation
                                   - Applicability of deep synthesis rules
                                   - AI ethics review requirements
                                   - Transparency and explainability of algorithms
                                
                                3. **Business Compliance**
                                   - Industry-specific regulations (e.g., customer service)
                                   - Consumer protection laws
                                   - Advertising law compliance
                                   - Sector-specific legal requirements
                                
                                4. **Intellectual Property Protection**
                                   - Core patent strategy recommendations
                                   - Trademark and copyright protection
                                   - Open-source software compliance
                                   - Third-party IP infringement risks
                                
                                5. **Contracts and Agreements**
                                   - Key points in customer service agreements
                                   - Data processing agreement templates
                                   - Vendor contracts
                                   - Employee NDAs
                                
                                Please provide specific compliance advice, legal risk assessments, and required legal documentation checklist.
                                """
                        )
                        .build(),
                // We currently don't have ParallelAgent, temporarily using ChatAgent
                ParallelAgent.builder()
                        .isMaster(true)
                        .name("expert_panel_agent")
                        .llmModel("default_llm")
                        .desc("Expert panel parallel evaluation")
                        .permittedToolNameList(Arrays.asList("tech_expert",
                                "business_expert",
                                "risk_expert",
                                "legal_expert"))
//                        .subAgents(Arrays.asList("tech_expert",
//                                "business_expert",
//                                "risk_expert",
//                                "legal_expert"))
                        .build()
        );
    }

    public static void main(String[] args) throws Exception {
        GlobalDefaultOxySpaceMapping.searchCurrentThreadStackAnnotationOxySpaceName(Thread.currentThread().getStackTrace()[1].getClassName());
        ServerApp.main(args);
    }
}
