const puppeteer = require('puppeteer')

;(async () => {
  try {
    const browser = await puppeteer.launch()
    const page = await browser.newPage()
    page.setExtraHTTPHeaders({
      'Accept-Language': 'de-DE,de;q=0.9,en-DE;q=0.8,en;q=0.7,en-US;q=0.6',
    })
    await page.goto(process.argv.slice(2)[0])

    const recipe = await page.evaluate(() => {
      const range = (x) => [...Array(x).keys()]
      const measures = Array.from(
        document.querySelectorAll('.row .kptn-ingredient-measure')
      ).map((a) => a.innerText)
      const ingredients = Array.from(
        document.querySelectorAll('.row .kptn-ingredient')
      ).map((a) => a.innerText)
      return {
        name: document.querySelector('.kptn-recipetitle').innerText,
        ingredients: range(Math.max(measures.length, ingredients.length)).map(
          (i) => {
            const [amount, unit] = measures[i]?.split(' ') ?? [null, null]
            return {
              'amount-desc': measures[i],
              name: ingredients[i],
              amount: parseFloat(amount),
              unit,
            }
          }
        ),
      }
    })

    console.log(JSON.stringify(recipe))

    await browser.close()
  } catch (e) {
    console.log(e)
  }
})()
