// Ported from gallery@main: calculate-hash skill (Apache 2.0)
function runTool(name, args) {
  const input = (args && args.input) || '';
  const encoded = new TextEncoder().encode(input);
  crypto.subtle.digest('SHA-256', encoded).then(function(buf) {
    const hex = Array.from(new Uint8Array(buf))
      .map(function(b) { return b.toString(16).padStart(2, '0'); })
      .join('');
    GallerySkill.postToolResult(JSON.stringify({ hash: hex, algorithm: 'sha-256' }));
  });
}
