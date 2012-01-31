(ns experiment.content
  (:use experiment.models.article))

(defn make-article [article]
  (create-article! (:name article) (:title article) (:body article)))

(defn make-articles []
  (doall
   (map
    make-article
    [{:name "about"
      :title "About PersonalExperiments.org"
      :body "
The goal of this site is to help users learn about themselves and share that learning with others.  We are exploring new methods of supporting users in building and running experiments, and in the future we aim to provide tools that improve the way we share our successes and failures with others.  The site is focused on supporting Personal Experiments.

## Who created the site?

This site is a joint project between three institutions and led by [Ian Eslick](http://ianeslick.com).  Our collaborators include the [New Media Medicine Group](http://newmed.media.mit.edu) of the [The MIT Media Laboratory](http://www.media.mit.edu/), the non-profit [Lybba](http://lybba.org/), and the [C3N Project](http://c3nproject.org) of the [Cincinnati Childrens Hospital and Medical Center](http://cincinnatichildrens.org/).  All of the participating institutions are dedicated to empowering patients through openness and encouraging increased patient engagement in the healthcare process.

## Why did we create it?

Science is simply a discipline we apply to our investigation of the
world to increase our confidence that our conclusions are reliable.
The scientific method outlines a very high level process of
hypothesis, prediction, experimentation, evaluation, and confirmation.
In medicine, the experimental model reaches it's most robust and
reliable form in the randomized, double-blind, placebo-controlled
clinical trial.  These trials have evolved to try to control all the
sources of error and ensure that we have the utmost confidence in the
result of a trial.

The problem with these trials today is that they do not provide a
personalized answer; these trials focus on determining how large
groups of people respond and not necessarily to predict who will have
side effects or who will do better or worse.  The single-subject
experimentation is an alternative experimental model which allows us
to evaluate a treatment just for ourselves, when it is safe to do so.
However, the results of these personal experiments do not directly
generalize to other people.  Just because we have the same symptoms,
what works for you may not work for me.

A further problem with large population clinical trials is that they
are expensive, and so most trials are run with a profit
motive (e.g. future drug sales).  There is nothing wrong with this,
but it does mean that unprofitable treatments, or unpopular
treatments, are rarely subject to trials and thus there is no
information for us, or our physicians, to work with.

## [A new model of discovery?](http://ianeslick.com/personalized-health-experiments-for-wellbeing)

Our longer term objective is to explore when and how we can take the
results of dozens of similar tests of a treatment and help someone
choose among multiple treatments - we don't need to know why something
works, we just want to know if it works and who it works for.  If we
have some evidence about who it works for, we can help people
prioritize their own exploration.

A Personal Experiment is an encapsulation of the scientific method and
also supports crowd-sourced peer review of each of the steps.  The
quality of instruments, experiments, and treatment protocols can all
be discussed and rated.  The opinions of many users can be used to
assess whether an experimental outcome is to be trusted or not.

This site does not directly support a model of hypothesis driven
research.  For example if we find out that eating butter increases
mental performance on tests, then we might want to know why.  Is it
the saturated fat, the medium chain fatty acids, the cholesterol, or
the trace proteins that is having the effect?  Hypothesis-driven
research would ask people to participate in experiments that would
separate which of the constituent elements was having an effect.
There are ethical and practical complexities to supporting this model
for a population, but an ambitious user could use the site to isolate
factors on their own.

Instead, we see the aims of a science of Collective Discovery as being
more modest; we want to find reliable predictors that help us know
which treatments improve symptoms for a given individual.  At it's
best, this is an engine for personalized medicine.  Moreover, reliable
patterns of effectiveness can also be used to suggest new avenues for
clinical investigation.

"}

{:name "treatment"
 :title "Making changes as Treatment"
 :body "
We are constantly changing what we do and observing the outcomes; it's how we all learned about world when we are children.  Changes can be anything such as a new kind of diet, a drug or supplement, or a new life habit such as meditation or attending church.  In this site, we  call any specific change that we can document a treatment.

## Search for treatments

![Treatment List](/img/intro/treatments.png)
"}

{:name "experiment"
 :title "What is a Personal Experiment?"
 :body "
An experiment is simply a protocol that specifies when and how long to try a treatment, how to record our symptoms, and how we will assess at the end whether the treatment changed the symptoms the way we hoped.

## It's more than just 'trying things out'

The challenge in making changes is that it can be hard to separate the impact of a treatment on a symptom from all the different things that can influence that symptom.  If we're hoping that a diet reduces fatigue, then we need to know that it isn't a lack of sleep that is causing us to feel tired.  Trying out the treatment multiple times helps to smooth over other causes that may interfere if we just try it out one time.

We also want to compare how we are doing on a treatment to how we're doing when we're off of it.  Being off a treatment is called a 'baseline', how we're doing in our normal course of life.  Our symptoms may vary even when we're not trying treatments, so the goal of a baseline period is to capture enough information to know if the changes during treatment are changing enough in the right direction to be a success.

## Example Experiment Screenshot

![Experiment Detail](/img/intro/experiment-detail.png)
"}

{:name "track"
 :title "Measuring your symptoms"
 :body "
Humans have poor memories of everyday occurrences.  Psychologists have identified many different sources of error in our recall.  This is why it is imperative to write down, as best we can, what is happening when it happens.  Of course this is hard to do if you're stuck with pencil and paper (or Excel), but fortunately there are myriad new technologies that can help simplify recording data from established methods like SMS messaging, to fancy devices like the [Zeo](http://myzeo.com) sleep monitor and the [FitBit](http://fitbit.com) activity tracker.  We support web, mobile web, e-mail, SMS, and devices that make their APIs public.

A measurement for a specific symptom is called an \"instrument\".  A symptom may be measured in multiple ways, so there may be multiple instruments for any given symptom.  Knowing how to measure things we care about can be hard, part of the role of this site is to help you share ways of measuring these things.

## ![Tracking measurements](/img/intro/trackers.png)
"}

{:name "share"
 :title "Evaluating and Sharing outcomes"
 :body "
When we've finished the experiment, we'll have a set of measurements for each period of the experiment (baseline and treatment periods) and now we'll want to analyze whether that treatment worked for us or not.  There are mathematical techniques that can estimate whether a result was 'significant', but we will be using visual techniques that help you identify when the measurements are meaningful.  For example, in the following prototype, any data point that lies outside the red or purple lines is significant.  Here the treatment (eating butter) leads to a significant decrease in test time (test-taking speed) over the space of a week.  The benefit persists for a few days, then receeds.

![apparent Control Chart](/img/intro/control-chart2.png)

## Sharing results

If you try a specific experiment, with the same instruments, treatment protocol, and evaluation as other people, then we can start to understand how well that treatment performs when tested that way.  Of course an experiment may not be a very good one and be prone to letting you conclude it works when it doesn't.  We support discussion and ratings for each experiment to help everyone chime in on whether the results of the experiment really reflect the effectiveness of the treatment.

![Discussion](/img/intro/sharing.png)
"}

{:name "design"
 :title "Design an Experiment"
 :body "
Support for designing your own experiments coming soon...
"}

])))

(defn terms-of-use []
  (create-article!
   "terms"
   "Terms of Use"
   "
## Effective December, 2011

The following terms constitute an agreement (the 'Agreement') between you and PersonalExperiments.org. This agreement governs your use of the Site, both as a casual visitor and a registered member, as described below.

By accessing or using the Site, you agree to be bound by the terms of this Agreement. If you register on the Site as a member, then you will be subject to the terms and conditions for both users and members.

## Membership Eligibility

Children under the age of 13 are prohibited from registering as members. By registering as a member you represent that you are age 13 or older.

PersonalExperiments.org has the right at any time to change or discontinue any aspect of this Site, including the content, hours of availability, and equipment needed for access to or use. You understand and agree that this Site is provided to you exclusively under these Terms of Use.  PersonalExperiments.org reserves the right to terminate your account at any time, for any reason, including if either party learns that you have provided false or misleading registration information or have violated the Terms of Use.

## This Site Does Not Provide Medical Advice

All of the material provided on the Site, such as text, treatments, dosages, outcomes, charts, patient profiles, graphics, photographs, images, advice, messages, forum postings, and any other material provided on the Site are for informational purposes only and are not a substitute for professional medical advice or treatment. Always seek the advice of your physician or other qualified health provider with any questions you may have regarding your health. Never disregard professional medical advice or delay in seeking it because of something you have read on this Site.

If you think you may have a medical emergency, call your doctor or 911 immediately. PersonalExperiments.org does not recommend or endorse any specific tests, physicians, products, procedures, opinions, or other information that may be mentioned on the Site. Reliance on any information provided by PersonalExperiments.org, by persons appearing on the Site at the invitation of the site administrators, or by other members is solely at your own risk.

## Acceptable and Lawful Use of Site by Members

Members shall not post or upload any information or other content on the Site that (a) is false, inaccurate or misleading; (b) is obscene or indecent; (c) infringes any copyright, patent, trademark, trade secret or other proprietary rights or rights of publicity or privacy of any party; or (d) is defamatory, libelous, threatening, abusive, hateful, or contains pornography. Members shall not interfere with other members' use and enjoyment of Site. Members may not use the Site to conduct any activity that is illegal or violates the rights of others, provide instructional information about illegal activities, or promote physical harm or injury against any group or individual.

All members represent and warrant that the information they provided when registering as a member, and all information that they subsequently provide regarding themselves and their membership, is true, accurate and not misleading.

## Use of Site by Members and Non-Members

You may not use any robot, spider, scraper, or other automated means to access the Site or content or services provided on the Site for any purposes. You may not post content on the Site that contains any viruses, Trojan horses, worms, time bombs, spiders, or other computer programming routines that are intended to damage, detrimentally interfere with, surreptitiously intercept or expropriate any system, data or personal information. You shall not attempt to make the Site unavailable through denial-of-service attacks or similar means. You shall not use contact information provided by members, or collect information about our members, to facilitate the sending of unsolicited bulk communications such as SPAM or SPIM or allow others use of your membership account to take such actions.

All the text, images, marks, logos, compilations (meaning the collection, arrangement and assembly of information) of the Web Site, including any Submissions (as defined below), and any of the foregoing sent to you by e-mail or other means (collectively, the 'Site Content') are proprietary to PersonalExperiments.org or to third parties where designated. Separate provisions apply to Health Data submitted by users and are defined below.

PersonalExperiments.org authorizes you to view, download, and print the Site Content subject to the following conditions: (a) you may only download and print the Site Content in limited quantities, as required to evaluate whether the good and services displayed on the Web Site are appropriate for your intended use; (b) you may not modify the Site Content; (c) any displays or print outs of the Site Content must be marked 'Copyright © 2011 PersonalExperiments.org. All rights reserved.'; and (d) you may not remove any copyright, trademark or other proprietary notices that have been placed in the Site Content. Except as expressly permitted above, copying, modifying reproduction, redistribution, republication, uploading, posting, transmitting, distributing or otherwise exploiting in any way the Site Content, or any portion of the Site Content, is strictly prohibited without the prior written permission of PersonalExperiments.org. In addition, you may not link to any part of the Web Site or any Site Content or frame or otherwise display in any manner the Site Content at any other web site or elsewhere without our prior written consent.

## Non-Commercial Use by Members

The Member Area and the content and information contained in the Member Area is for the personal use of individual members only and may not be used in connection with any commercial endeavors. Organizations, companies, and/or businesses may not become members and should not use the Site without the written pre-authorization from PersonalExperiments.org.

## Privacy

You agree that you have read, understood and accept the terms of PersonalExperiments.org Privacy Policy, for the use of the Site (the 'Privacy Policy') This Policy governs the collection, use and sharing of personal and non-personal information for all those who elect to use the Site including Submissions.

## Submissions

PersonalExperiments.org welcomes your comments regarding the Site and Site Content. In the event that you submit ideas, suggestions, materials or other information to PersonalExperiments.org, whether at the request of MIT Media Lab and LTA or through your use of the Site (all of the foregoing being 'Submissions'), the Submissions will be deemed, and will remain, the sole property of PersonalExperiments.org. The Submissions will not be treated as confidential by PersonalExpeirment.org, or their respective Affiliates (as defined below), nor will such parties be liable for any use or disclosure of any Submissions. Without limiting the foregoing, PersonsalExperiments.org own all presently known or hereafter existing rights to the Submissions of every kind, in perpetuity, and will be entitled to unrestricted use of the Submissions for any lawful purpose whatsoever, commercial or otherwise, by any means, by any media, without compensation to the provider, author, creator or inventor of the Submissions (the 'Participant'). Each Participant irrevocably and unconditionally waives and covenants not to assert any such rights against PersonalExperiments.org or its affiliates, employees, departments, successors, assigns, licensees, and customers (collectively, 'PersonalExperiments.org Affiliates'), as well as any users of the Site.

## Posting and Use of Content in the Member Area

The Member Area includes community areas, such as forums and other member areas, where members may post messages, images, and other content. If you are a member, you (or the author) own(s) the copyright in the messages, images, and other content you post in the Member Area. However, by posting such content to the Member Area, you grant PersonalExperiments.org and its respective Affiliates the right to use, copy, display, perform, distribute, translate, edit, and create derivative works of your postings, subject to the terms of the Privacy Policy.

While some community areas in the Member Area are monitored periodically for topicality, PersonalExperiment.org have no obligation to prescreen postings and are not responsible for their content. We encourage you to notify us of inappropriate or illegal content and we reserve the right to remove postings for any reason.

You agree not to disclose to any person or entity personally identifiable information about other members that you learn while using this Site (whether posted in the Member Area by a member or emailed to you by a member) without the written consent of such member. You may disclose information of a general nature that will not reasonably lead to the identification of the member who provided such information or whom such information is about, to third parties outside this Site, subject to the above restriction on non-commercial use.

## Member Password and Login Identity

You are responsible for maintaining the confidentiality of your member password and login, and are fully responsible for all activities that occur under your password or account with or without your knowledge. If you knowingly provide your login and password information to another person, your membership privileges may be suspended temporarily or terminated. You agree to immediately notify PersonalExperiments.org of any unauthorized use of your membership password or login or any other breach of security.

## Health Data

This site encourages you to submit and share data about your health and history. You have specific rights to any personal health data you have entered including changing, deleting, downloading, and sharing this data. The use of your data by PersonalExperiment.org are governed by the Privacy Policy. Your personal data will be used by the Site to construct an aggregate, de-identified view of the patient population. You agree not to enter personal identifiers into any survey or journal forms on the Site. Only the profile section should contain identifiers.

Health Data does not include any information or comments in the forums or public commenting section which are to be considered public and are governed as described in Submissions above. Private contact between users can be done via an e-mail exchange to facilitate non-public discourse.

Health data can be removed from future aggregate views on the site, but your prior data will be maintained in an archived, de-identified form. To have your data and identifiers removed from active use in the system, send an e-mail with your instructions to ianeslick@gmail.com.

## Links to Third Party Web Site Are Not Endorsements

The Site contains links to third-party web sites. The linked sites are not under our control, and we are not responsible for the content of any linked site. We provide these links as a convenience only, and a link does not imply endorsement of, sponsorship of, or affiliation with the linked site by PersonalExperiments.org. Links to merchants or advertisers are owned and operated by independent retailers or service providers, and therefore, we make no warranties or representations regarding their products, services or business practices. You are encouraged to exercise appropriate due diligence before proceeding with any transaction with any of these third parties.

## Membership Termination

You agree that PersonalExpeirments.org may, with or without cause, immediately terminate your membership and access to the Member Area, without prior notice. Without limiting the foregoing, reasons for termination may include, but are not limited to, the following: (a) breaches or violations of this Agreement or other incorporated agreements or policies, (b) requests by law enforcement or other government agencies, (c) a request by you (self-initiated membership cancellation), (d) unexpected technical issues or problems, and (e) extended periods of inactivity. PersonalExperiments.org have no obligation to maintain, store, or transfer information or data to you that you have posted on or uploaded to the Site.

## Modifications to this Agreement

We reserve the right to modify this Agreement at any time, and without prior notice, by posting amended terms on this Site. We encourage you to review this Agreement periodically for any updates or changes, which will be reflected by the revised date of the Agreement.

## Disclaimer of Warranties

The Site and the content and services made available on the Site are provided on an 'as is' and 'as available' basis. PersonalExperiments.org disclaims all express and implied warranties and representations, including, but not limited to, any implied warranty of fitness for a particular purpose, with regard to the Site, the Site content, or any advice or services provided through the Site to the extent permitted by law. PersonalExperiment.org does not warrant that access to the Site or its content or services will be uninterrupted or error-free or that defects in the Site will be corrected.

The advice, recommendations, information, and conclusions posted or emailed by other members of the Site are not in any way vetted, approved or endorsed by PersonalExperiments.org. You acknowledge that you use such information at your own risk.

## Limitation of Liability

Under no circumstances shall PersonalExperiment.org and their respective Affiliates be liable for any indirect, incidental, special, or consequential damages (even if it has been advised of the possibility of such damages) due to your use of this Site or to your reliance on any of the content contained or the services provided therein.

## Indemnification

You agree to indemnify, defend, and hold harmless PersonalExperiments.org and their respective Affiliates, from and against any claims, actions or demands, damages, expenses, liabilities and settlements, including without limitation, attorneys’ fees and other costs incurred or resulting from, or alleged to result from, your violation of this Agreement.

## International Users

PersonalExperiments.org does not warrant that it is appropriate, to download, store or disclose information on the Site outside of the United States ('International Use'), in compliance with the user’s local jurisdiction. Personal information ('Information') that is submitted to this Site will be collected, processed, stored, disclosed and disposed of in accordance with applicable U.S. law and our Privacy Policy. If you are an International User, you acknowledge and agree that PersonalExperiments.org may collect and use your Information and disclose it to other entities outside your resident jurisdiction. In addition, such Information may be stored on servers located outside your resident jurisdiction. By providing us with your Information, you acknowledge that you consent to the transfer of such Information outside your resident jurisdiction, as detailed in our Privacy Policy. If you do not consent to such transfer, you may not use this Site.

## Member Notices

If you register as a member, you agree that PersonalExpeirments.org may send notices to you by email at the email address you provide when registering to become a member (or which you later update using the functionality of the Site).

## Governing Law and Venue

Those who choose to access this website do so at their own risk and on their own initiative and are responsible for compliance with all applicable local laws. These terms shall be governed by and construed in accordance with the California State Laws. Any dispute under these terms shall be subject to the exclusive jurisdiction of the courts of California (subject to appeal) and, by using this website, you hereby submit to the jurisdiction of such courts for such purposes and waive any and all objections as to jurisdiction or venue in such courts.

## Digital Millennium Copyright Act

The Digital Millennium Copyright Act (17 U.S.C. §512, as amended) governs the operation and maintenance of this Site. 

## No Waiver

No delay or failure to act by PersonalExperiments.org in exercising any of its respective rights occurring upon any noncompliance or default by you, with respect to any of the terms and conditions of this Agreement, will impair any such right or be construed to be a waiver thereof, and a waiver by PersonalExperiments.org of any of the covenants, conditions or agreements to be performed by you, will not be construed to be a waiver of any succeeding breach thereof or of any other covenant, condition or agreement hereof contained. As used in this Agreement, 'including' means 'including but not limited to.'

## Severability

If any provision of this Agreement is found by a court of competent jurisdiction to be invalid, illegal or unenforceable, under any present or future law, then as long as each party’s rights and obligations under this Agreement are not materially or adversely affected thereby, such provisions shall be fully severable, and the remaining provisions of the Agreement will remain in full force and effect to the greatest extent permitted by law. Except as otherwise expressly provided herein, this Agreement sets forth the entire agreement between you and PersonalExperiments.org regarding its subject matter, and supersedes all prior promises, agreements or representations, whether written or oral, regarding such subject matter. You agree that the electronic text of this Agreement constitutes a 'writing', and your assent to the terms and conditions hereof constitutes a 'signing' for all purposes.

## Use of Name

Users of the Site shall not use the name of 'PersonalExperiments.org', 'Ian Eslick', the 'Massachusetts Institute of Technology', the 'MIT Media Laboratory', or any variation, adaptation or abbreviation thereof, or of any of its trustees, officers, faculty, students, employees or agents, or any trademark owned by MIT, or any terms of this Agreement I any promotional material or public announcement or disclosure, without the prior written consent of MIT’s Technology Licensing Office.

All other brand names and logos that appear throughout PersonalExperiment.org web pages are marks owned by third parties.

All software used on the Site is proprietary to us or to third parties, and except as may be required to exercise the foregoing license, any redistribution, sale, decompilation, reverse engineering, disassembly, translation or other reduction of such software to human-readable form is prohibited.

You agree, represent and warrant, that your use Site and the Site Content, or any portion thereof, will be consistent with the foregoing license, covenants and restrictions and will neither infringe, nor violate the rights of any other party or breach any contract or legal duty to any other parties. In addition, you agree that you will comply with all applicable laws and regulations (the 'Laws') relating to the Site, the Site Content or your use of the same, and you will be solely responsible for your violations of any of the Laws.

## Assignment

PersonalExperiments.org may assign this Agreement, at any time, to a subsidiary, parent company or a successor to its business, as part of a merger or sale of substantially all of its assets. You may not assign or transfer this Agreement.

If you have questions or comments about our Terms of Use, please contact us at: ianeslick@gmail.com. You can also direct questions or comments, or violations of this Agreement, to:

Ian Eslick
MIT Media Laboratory
E15-274G
77 Massachusetts Avenue
Cambridge, MA 02139
USA
"))

(defn privacy []
  (create-article!
   "privacy"
   "Privacy Policy"
   "
## Effective December 11th, 2011

PersonalExperiments.org acknowledges that our website, www.personalexperiments.org (the “Site”), can only provide the Community with an effective tool if we build trust among members. This privacy policy (the “Policy” or “Privacy Policy”) outlines the type of information PersonalExperiments.org collects from our members and how it is shared with other parties. We reserve the right to modify this policy at any time, and without prior notice, by posting amended terms on this website. We encourage you to review this policy periodically for any updates.

## WHAT KIND OF DATA WE COLLECT AND WHY

PersonalExperiments.org enables patients to provide information about their disease history, family history and lifestyle online so that the collective knowledge of thousands of patients can be shared with others affected by the disease, as well as researchers. Our collective success in achieving this goal depends on members' willingness to share their data with others.

The type of data patients may add to their profiles ('Profile Data')  may include: 

*   Condition/disease information, including diagnosis date, first symptom information, and family history
*   Treatments tried, including treatment start dates, stop dates, and dosages
*   Symptoms experienced, including severity and duration
*   Biographical information, including age, location (city, state & country), and general notes.
*   Experimental data recorded from self-experiments run on the site

Patients may voluntarily enter information that identifies them ('Personally Identifiable Information' such as name, address, email, and birth date) into their Profile Data. PersonalExperiments.org will hold Profile Data and personally identifiable information provided by patients in the strictest confidence and maintain the security of such information in accordance with all applicable laws. Such data will only be available to the site manager and will not be released to any parties in any form that will identify the patient, without the patient’s prior written authorization. You will be asked for an email address and to make a selection of preferences about how your personal information can be used to contact you for administrative and research purposes. For example, you can authorize researchers to contact you with information about drug trials or requests for further information.

The site allows patients and researchers to make their own surveys and requests for data. Data entered into any survey in the Collect section of the website is available to the rest of the community in aggregate form as described above. You should never enter Personally Identifiable Information into any of these forms if you are concerned about privacy. Please comment on any forms that you feel violate common sense protection of privacy.

We may periodically ask you to complete short surveys about the site itself. Your participation in these surveys is not required, and your refusal to do so will not impact your experience on the website. Our aim is to create the best possible tool for research, as well as provide global information for patients. We would appreciate any input on site surveys etc. that you are willing to provide.

Cookies are required to use the PersonalExperiments.org service. A cookie is a small data file, which often includes an anonymous unique identifier that is sent to your browser from a web site's computers and stored on your computer's hard drive. PersonalExperiments.org uses two types of cookies: Session Cookies, and Persistent Cookies. Session Cookies are temporary cookies that remain in the cookie file of your browser until you leave the Website. Session cookies do no store any personally identifying information, only a unique visitor ID number that we can use to customize the PersonalExperiments.org functionality for you. Persistent Cookies remain in the cookie file of your browser for much longer, even after you leave the PersonalExperiments.org website. Persistent cookies do no store any personally identifying information, only preferences that should persist from visit to visit, like 'Remember Me'.

## HOW YOUR PERSONALLY IDENTIFIABLE DATA IS USED

In order to help researchers and to learn from our collective experiences, your Survey and Experiment data will be aggregated and made available to other members in aggregate non-identifiable form. For example, we may report the number of patients trying a particular treatment or the number of patients experiencing a particular symptom. This aggregate data will not include your personal information.

By completing your Profile Data, your personal information will be added to our database, which will be used for research purposes only. For example, we may look at scientific questions such as, 'Do certain experimental treatments seem associated with certain sub-populations of patients?' At PersonalExperiments.org and in conjunction with external researchers, we’re interested in better understanding the experience of living with chronic conditions and improving treatment options and health outcomes for everyone.

PersonalExperiments.org will not rent, sell or share information that  personally identifies you for marketing purposes. We may subsequently assign control of PersonalExperiments.org and all of its rights and obligations to another entity, to ensure its continued availability and use. You will receive timely notice of such an assignment, and have the option of deleting your personal information prior to such a transfer.

PersonalExperiments.org’s goal is to allow researchers direct and anonymous communication with you through our system. Furthermore, we may also provide researchers with anonymized data (which may or may not be aggregated) for research purposes only. Personal identifiers will be used only to contact and to de-duplicate information and will not be available to anyone without your explicit permission.

## OTHER SECURITY ISSUES

PersonalExperiments.org will take reasonable measures to safeguard your personal information and identity from any other members with whom you may interact in the course of using the Site or who may have access to information you have posted in the Discuss area. Researchers requesting access to the data on PersonalExperiments.org will be personally vetted by PersonalExperiments.org staff or approved partners.

At some point we may choose to implement a method by which we can increase confidence in the identity of a user of PersonalExperiments.org (a researcher, a patient, a family member, etc.). This method will involve trusted individuals providing a reference for another individual. We and our partners may also take the extra step of directly identifying researchers and limiting access to any researchers who have not gone through this process.

## RISKS AND BENEFITS

A key objective in providing the PersonalExperiments.org platform is to improve health outcomes and expedite the development of successful treatments for a wide variety of conditions. Although there are no certain benefits to using the Site, by pooling information and resources, we hope that PersonalExperiments.org will foster the necessary access to otherwise un-captured global patient data that can accelerate the pace of personal and clinical discovery.

Users may elect to stop using the Site at any time. You are free to skip any questions or data fields that make you feel uncomfortable. You will have full control over your information, including the ability to download, remove all personal identifiers, or to remove data from the system subject to the conditions described below.

## QUESTIONS ABOUT THE PRIVACY POLICY

If you have questions or comments about our Privacy Policy, please contact us at: ianeslick@gmail.com. You can also direct questions or comments to:

PersonalExperiments.org c/o Ian Eslick
MIT Media Laboratory
77 Masachusetts Avenue, E15-274G 
Cambridge, MA 02139
USA
"))

(defn study1-docs []
  (doall
   (map
    make-article
    [{:name "study1-protocol"
      :title "Authoring Single-Subject Experimental Protocols"
      :body "
## Procedure  
 
1.  Register at the site http://experiment.media.mit.edu by clicking on \"Authoring an Experiment\" on the home page.  [This requires at a minimum your e-mail address for follow up contact during the study period (ending in March of 2012).  After that, personal identifiers will be removed from the data we collect in this study unless you opt-in to keep your account]
2.  Log-in to the site and read introductory material about single-subject experiments.  This should take no more than 30 minutes.
3.  Click on \"Author an Experiment\".  [Each authoring activity may take from between 15 minutes to 2 hours depending on how extensively you wish to perform the online research].  Authoring consists of:
a.  Reading patient reports of trying different treatments
b.  Go to websites that describe the treatment and/or symptoms as well as potential factors that would lead to false conclusions and how to adjust for them.
c.  Fill out the template to define the experiment
4.  When you are done, click on \"Take the Survey\" to take an exit survey.  This survey will be hosted at http://qualtrics.com

## Authoring an Experiment

The primary goal of this study is to educate patients on running single-subject experiments in their own lives and understanding what aspects of this are easy or accessible, and which are more difficult.  This will help inform future work we do on the site to support users who want to understand whether the changes they want to make in their life will have the impact they hope.

An experiment is made up of a number of different parts, described in more detail in the introductory materials.

-   Outcome variables and how to measure them
-   Confounding variables and how to measure them
-   Description of treatment protocol 
-   Explicit dosing information, if applicable   
-   The schedule for baseline and intervention including onset and washout times
-   The method of analysis (e.g. difference of means, control charts)
-   Pre-conditions (e.g. what symptoms need to be present for treatment to be relevant, including prior diagnoses, symptoms, etc)
-   Possible predictors of a positive response ('People who do X usually find this works')
"}
     ])))


(defn bootstrap-articles []
  (make-articles)
  (privacy)
  (terms-of-use))