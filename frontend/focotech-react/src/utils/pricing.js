/** Server prices take precedence; fallback tiers are estimates until the catalog quote loads. */
export const getCoursePricing = (course) => {
  let price = typeof course?.price === 'number' ? course.price : Number(String(course?.price || '').replace(/[^\d.]/g, ''));
  if (!Number.isFinite(price) || price <= 0) {
    const title = (course?.title || '').toLowerCase(); const id = Number(course?.courseId || course?.id);
    price = title.includes('machine learning') || title.includes('ai') || title.includes('deep learning') || id === 4 ? 899
      : title.includes('spring') || title.includes('java') || title.includes('backend') || id === 5 ? 799
      : title.includes('database') || title.includes('sql') || title.includes('postgres') || id === 2 ? 699 : 499;
  }
  return { price, origPrice: price, formatted: `₹${price.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`, formattedOrig: '', discount: '' };
};
