const { chromium } = require('playwright');
const fs = require('fs');

async function captureUI() {
  const browser = await chromium.launch({ headless: false });
  const context = await browser.newContext();
  const page = await context.newPage();
  
  try {
    console.log('Navigating to Angular app on http://localhost:4205');
    await page.goto('http://localhost:4205', { waitUntil: 'networkidle' });
    
    // Wait for the app to load
    await page.waitForSelector('app-enhanced-chat', { timeout: 10000 });
    
    console.log('Taking full page screenshot...');
    await page.screenshot({ 
      path: 'ui-full-page.png', 
      fullPage: true 
    });
    
    console.log('Taking viewport screenshot...');
    await page.screenshot({ 
      path: 'ui-viewport.png'
    });
    
    // Capture specific sections
    console.log('Capturing header section...');
    const header = await page.locator('mat-toolbar').first();
    if (await header.isVisible()) {
      await header.screenshot({ path: 'ui-header.png' });
    }
    
    console.log('Capturing sidebar section...');
    const sidebar = await page.locator('.thread-sidebar').first();
    if (await sidebar.isVisible()) {
      await sidebar.screenshot({ path: 'ui-sidebar.png' });
    }
    
    console.log('Capturing chat section...');
    const chat = await page.locator('app-enhanced-chat').first();
    if (await chat.isVisible()) {
      await chat.screenshot({ path: 'ui-chat.png' });
    }
    
    // Check for busy/cluttered elements
    console.log('Analyzing UI elements...');
    
    const buttons = await page.locator('button').count();
    const chips = await page.locator('mat-chip').count();
    const icons = await page.locator('mat-icon').count();
    const formFields = await page.locator('mat-form-field').count();
    const toggles = await page.locator('mat-slide-toggle').count();
    
    const analysis = {
      totalButtons: buttons,
      totalChips: chips,
      totalIcons: icons,
      totalFormFields: formFields,
      totalToggles: toggles,
      timestamp: new Date().toISOString(),
      url: 'http://localhost:4205'
    };
    
    console.log('UI Element Analysis:');
    console.log(`- Buttons: ${buttons}`);
    console.log(`- Chips: ${chips}`);
    console.log(`- Icons: ${icons}`);
    console.log(`- Form Fields: ${formFields}`);
    console.log(`- Toggles: ${toggles}`);
    
    // Save analysis to file
    fs.writeFileSync('ui-analysis.json', JSON.stringify(analysis, null, 2));
    
    // Get visible text elements to check for clutter
    const visibleText = await page.textContent('body');
    const textLength = visibleText.length;
    console.log(`- Total visible text length: ${textLength} characters`);
    
    // Check for specific UI sections that might be busy
    const headerHeight = await header.boundingBox().then(box => box?.height || 0);
    const chatArea = await page.locator('.chat-messages').first();
    const chatHeight = await chatArea.isVisible() ? 
      await chatArea.boundingBox().then(box => box?.height || 0) : 0;
    
    console.log(`- Header height: ${headerHeight}px`);
    console.log(`- Chat area height: ${chatHeight}px`);
    
    // Take a screenshot after some interaction
    console.log('Taking screenshot with expanded search...');
    const searchToggle = await page.locator('.search-toggle').first();
    if (await searchToggle.isVisible()) {
      await searchToggle.click();
      await page.waitForTimeout(1000);
      await page.screenshot({ path: 'ui-with-search.png' });
    }
    
    console.log('Screenshots saved successfully!');
    console.log('Analysis saved to ui-analysis.json');
    
  } catch (error) {
    console.error('Error capturing UI:', error.message);
    
    // Take screenshot of error state
    await page.screenshot({ path: 'ui-error-state.png' });
  } finally {
    await browser.close();
  }
}

captureUI();